package com.aritxonly.bdfun.data

import android.content.Context
import android.net.Uri
import android.text.Html
import com.aritxonly.bdfun.BuildConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt

class BiliApi(context: Context) : DynamicFeedSource {
    private val appContext: Context = context.applicationContext
    val cookieJar = BiliCookieJar(context)

    private val client = HttpClients.base.newBuilder()
        .cookieJar(cookieJar)
        .build()

    private val buvidMutex = Mutex()
    private val wbiMutex = Mutex()
    /** 消息会话昵称/头像后台补全：不阻塞首屏列表。 */
    private val backgroundScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO,
    )
    /** 历史记录补播放量/弹幕数的缓存：bvid -> (play, danmaku)，最多 300 条。 */
    private val videoStatCache = object : LinkedHashMap<String, Pair<Long, Long>>(300, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Pair<Long, Long>>?,
        ): Boolean = size > 300
    }

    private fun cachedVideoStat(bvid: String): Pair<Long, Long>? =
        synchronized(videoStatCache) { videoStatCache[bvid] }

    private fun putVideoStat(bvid: String, stat: Pair<Long, Long>) {
        synchronized(videoStatCache) { videoStatCache[bvid] = stat }
    }

    /** 消息卡片补封面 / UP 主用的小缓存：bvid -> (封面, UP 主 mid)。 */
    private data class MessageVideoBrief(val cover: String, val ownerMid: Long)

    private val messageVideoCache =
        object : LinkedHashMap<String, MessageVideoBrief>(64, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MessageVideoBrief>?,
            ): Boolean = size > 64
        }

    @Volatile
    private var wbiKeys: WbiKeys? = null

    /** buvid 获取失败后的冷却时间，避免每个请求都重新卡在一次失败请求上。 */
    @Volatile
    private var buvidRetryAt = 0L

    /** WBI key 的获取时间；key 会轮换，进程活太久时要重新拉。 */
    @Volatile
    private var wbiFetchedAt = 0L

    val isLoggedIn: Boolean get() = cookieJar.hasSession
    val mid: Long get() = cookieJar.mid
    val csrf: String get() = cookieJar.csrf

    // ---------------------------------------------------------------- auth

    suspend fun nav(): UserProfile? = withContext(Dispatchers.IO) {
        val root = rawRequest("/x/web-interface/nav", emptyMap())
        parseWbi(root.optJSONObject("data")?.optJSONObject("wbi_img"))
        val data = root.optJSONObject("data") ?: return@withContext null
        if (!data.optBoolean("isLogin")) return@withContext null
        val level = data.optJSONObject("level_info")?.optInt("current_level") ?: 0
        val vip = data.optJSONObject("vip_label")
        UserProfile(
            mid = data.l("mid"),
            name = data.s("uname"),
            face = avatar(data.s("face"), 160),
            sign = data.s("sign"),
            level = level,
            coins = data.optDouble("money", 0.0),
            vip = data.optInt("vipStatus") == 1,
            vipLabel = vip?.s("text").orEmpty(),
        )
    }

    /**
     * App / TV 端扫码登录：比网页扫码多拿一个 access_key。
     * access_key 存在 cookieJar 里，`signAppParams` 会自动带上，
     * `/x/v2/space` 这类 App 接口才能返回 IP 属地等字段。
     */
    suspend fun qrGenerate(): QrLogin = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "local_id" to "0",
            "mobi_app" to "android",
            "platform" to "android",
        )
        signAppParams(params, TV_APP_KEY, TV_APP_SEC, includeAccessKey = false)
        val data = dataRequest(
            path = "/x/passport-tv-login/qrcode/auth_code",
            params = params,
            base = PASSPORT,
            method = "POST",
        )
        QrLogin(url = data.s("url"), key = data.s("auth_code"))
    }

    suspend fun qrPoll(key: String): QrPoll = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "auth_code" to key,
            "local_id" to "0",
            "mobi_app" to "android",
            "platform" to "android",
        )
        signAppParams(params, TV_APP_KEY, TV_APP_SEC, includeAccessKey = false)
        val root = rawRequest(
            path = "/x/passport-tv-login/qrcode/poll",
            params = params,
            base = PASSPORT,
            method = "POST",
        )
        when (root.optInt("code", -1)) {
            0 -> {
                val data = root.optJSONObject("data") ?: return@withContext QrPoll(
                    QrState.Error,
                    "登录失败：没有返回登录信息",
                )
                saveTvLogin(data)
                val profile = nav()
                QrPoll(QrState.Success, "登录成功", profile)
            }
            86039, 86101 -> QrPoll(QrState.Waiting, "等待扫码")
            86090 -> QrPoll(QrState.Scanned, "已扫码，请在手机上确认")
            86038 -> QrPoll(QrState.Expired, "二维码已失效")
            else -> QrPoll(
                QrState.Error,
                root.optString("message").ifBlank { "登录失败" },
            )
        }
    }

    private fun saveTvLogin(data: JSONObject) {
        val cookies = data.optJSONObject("cookie_info")
            ?.optJSONArray("cookies")
            .objects()
        cookies.forEach { cookie ->
            val name = cookie.s("name")
            val value = cookie.s("value")
            if (name.isNotBlank() && value.isNotBlank()) cookieJar.put(name, value)
        }
        // access_key 也存进同一个 cookie 容器，App 签名和退出登录都走它。
        val accessKey = data.s("access_token")
            .ifBlank { data.optJSONObject("token_info")?.s("access_token").orEmpty() }
        if (accessKey.isNotBlank()) cookieJar.put("access_key", accessKey)
    }

    fun logout() {
        cookieJar.clear()
        wbiKeys = null
        wbiFetchedAt = 0L
        buvidRetryAt = 0L
    }

    private fun saveLoginCookies(url: String) {
        val uri = Uri.parse(url)
        listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5").forEach { name ->
            uri.getQueryParameter(name)?.takeIf { it.isNotBlank() }?.let { cookieJar.put(name, it) }
        }
    }

    // ---------------------------------------------------------------- feed

    suspend fun recommend(freshIndex: Int = 1, web: Boolean = true): List<VideoItem> = withContext(Dispatchers.IO) {
        var primaryError: Exception? = null
        val primary = try {
            if (web) webRecommend(freshIndex) else appRecommend(freshIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            primaryError = e
            emptyList()
        }
        if (primary.isNotEmpty()) {
            LogCollector.record(
                "FEED",
                "source=${if (web) "web" else "app"} count=${primary.size} " +
                    "first=${primary.firstOrNull()?.bvid}",
            )
            return@withContext primary
        }

        // 纯网络错误说明整个网络都不通，立刻报错，避免再用另一个接口白等一轮。
        val netError = primaryError
        if (netError is BiliException && netError.code == -1) throw netError

        // Web 推荐失败/为空时用 App 推荐兜底，反之亦然；总比整页空白强。
        val fallback = try {
            if (web) appRecommend(freshIndex) else webRecommend(freshIndex)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
        if (fallback.isNotEmpty()) {
            LogCollector.record(
                "FEED",
                "source=${if (web) "web->app-fallback" else "app->web-fallback"} " +
                    "count=${fallback.size} first=${fallback.firstOrNull()?.bvid}",
            )
            return@withContext fallback
        }
        if (primaryError != null) throw primaryError
        emptyList()
    }

    private suspend fun webRecommend(freshIndex: Int): List<VideoItem> {
        val data = dataRequest(
            "/x/web-interface/wbi/index/top/feed/rcmd",
            mapOf(
                "web_location" to "1430650",
                "y_num" to "5",
                "fresh_type" to "4",
                "feed_version" to "V8",
                "fresh_idx_1h" to freshIndex.toString(),
                "fetch_row" to "1",
                "fresh_idx" to freshIndex.toString(),
                "brush" to "0",
                "homepage_ver" to "1",
                "ps" to "12",
            ),
            signed = true,
        )
        return data.optJSONArray("item").objects().mapNotNull(::parseFeedVideo)
    }

    private suspend fun appRecommend(freshIndex: Int): List<VideoItem> {
        // App 推荐域名是 app.bilibili.com；用 api.bilibili.com 会 404，随后悄悄退回 Web 推荐。
        val data = dataRequest(
            "/x/v2/feed/index",
            mapOf(
                "build" to "7880300",
                "mobi_app" to "android",
                "platform" to "android",
                "idx" to freshIndex.toString(),
                "login_event" to "0",
                "network" to "wifi",
                "style" to "2",
                "ps" to "12",
            ),
            base = APP,
        )
        val out = data.optJSONArray("items").objects().mapNotNull { raw ->
            val args = raw.optJSONObject("args")
                ?: runCatching { JSONObject(raw.optString("args")) }.getOrNull()
                ?: JSONObject()
            val player = raw.optJSONObject("player_args")
            val cardGoto = raw.s("card_goto")
            val goto = raw.s("goto")
            // App 推荐里混着直播、广告、横幅、游戏卡和竖屏短视频；
            // 竖屏卡片的 aid 是 12 位以上，标准播放接口打不开，这里只要普通稿件。
            val isVideoCard = cardGoto == "av" || goto == "av" ||
                (cardGoto.isEmpty() && goto.isEmpty() &&
                    raw.s("card_type").startsWith("small_cover"))
            if (!isVideoCard) return@mapNotNull null
            val aid = args.l("aid")
                .takeIf { it > 0L }
                ?: player?.l("aid")?.takeIf { it > 0L }
                ?: raw.s("param").toLongOrNull()
                ?: 0L
            // av↔bv 公式只支持 32 位 aid；超出的都是竖屏/广告卡片。
            if (aid !in 1L..2_147_483_647L) return@mapNotNull null
            val directBvid = raw.s("bvid").ifBlank { args.s("bvid") }
                .ifBlank { player?.s("bvid").orEmpty() }
            val bvid = directBvid.ifBlank { aidToBvid(aid) }
            if (bvid.isBlank()) return@mapNotNull null
            VideoItem(
                bvid = bvid,
                aid = aid,
                title = cleanHtml(raw.s("title")),
                cover = image(raw.s("cover"), 480),
                ownerName = args.s("up_name").ifBlank { args.s("name") },
                ownerFace = avatar(args.s("up_face").ifBlank { args.s("face") }, 96),
                ownerMid = args.l("up_id").takeIf { it > 0 } ?: args.l("mid"),
                play = parseCountText(raw.s("cover_left_text_1")),
                danmaku = parseCountText(raw.s("cover_left_text_2")),
                duration = player?.optInt("duration", 0)
                    ?.takeIf { it > 0 }
                    ?: parseDurationText(raw.s("cover_right_text")),
                pubdate = 0,
                cid = player?.l("cid")?.takeIf { it > 0 } ?: 0,
                categoryName = args.s("tname").ifBlank { raw.s("tname") },
                appRecommendation = true,
                dislikeReasons = (raw.optJSONArray("three_point_v2").objects()
                    .filter { it.s("type") == "dislike" }.flatMap { it.optJSONArray("reasons").objects() } +
                    raw.optJSONObject("three_point")?.optJSONArray("dislike_reasons").objects())
                    .map { RecommendationReason(it.l("id"), it.s("name")) }.distinctBy { it.id },
                isFollowed = args.optBoolean("is_followed", false) ||
                    raw.optBoolean("is_followed", false),
            )
        }
        val rawCount = data.optJSONArray("items")?.length() ?: 0
        LogCollector.record("FEED", "app-raw=$rawCount kept=${out.size}")
        return out
    }

    suspend fun dislikeRecommendation(video: VideoItem, reason: RecommendationReason) {
        if (video.appRecommendation) {
            check(cookieJar.value("access_key").isNotBlank()) { "请重新扫码登录以提交 App 推荐反馈" }
            val params = mutableMapOf("goto" to "av", "id" to video.aid.toString(),
                "reason_id" to reason.id.toString(), "build" to "1", "mobi_app" to "android")
            signAppParams(params)
            dataRequest("/x/feed/dislike", params, base = APP)
        } else {
            postRequest("/x/web-interface/feedback/dislike", mapOf(
                "app_id" to "100", "platform" to "5", "from_spmid" to "", "spmid" to "333.1007.0.0",
                "goto" to "av", "id" to video.aid.toString(), "mid" to video.ownerMid.toString(),
                "track_id" to video.recommendationTrackId, "feedback_page" to "1",
                "reason_id" to reason.id.toString(), "csrf" to csrf), tolerateEmptyBody = false)
        }
    }

    suspend fun popular(page: Int = 1): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/popular",
            mapOf("pn" to page.toString(), "ps" to "20"),
        )
        data.optJSONArray("list").objects().mapNotNull(::parseFeedVideo)
    }

    suspend fun ranking(rid: Int = 0): List<VideoItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/ranking/v2",
            mapOf("rid" to rid.toString(), "type" to "all"),
        )
        data.optJSONArray("list").objects().mapNotNull(::parseFeedVideo)
    }

    // ---------------------------------------------------------------- video

    suspend fun view(bvid: String): VideoDetail = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/web-interface/view", mapOf("bvid" to bvid))
        parseVideoDetail(data)
    }

    /**
     * 按 aid 查视频。
     *
     * 2026 年起 B 站开始使用 64 位的新 av 号（例如 117535471047131），
     * 老的四则运算 av→BV 公式只适用于 32 位旧号，新号算出来是错的 BV。
     * 消息中心这类只给 aid 的地方必须走这个接口让服务器换 BV。
     */
    suspend fun viewByAid(aid: Long): VideoDetail = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/web-interface/view", mapOf("aid" to aid.toString()))
        parseVideoDetail(data)
    }

    suspend fun playInfo(
        detail: VideoDetail,
        preferredQuality: Int,
        cid: Long = detail.cid,
        durationSec: Int = detail.duration,
        codecPreference: String = "auto",
        cdnService: String = "base",
    ): PlayInfo = withContext(Dispatchers.IO) {
        val params = mapOf(
            "bvid" to detail.bvid,
            "cid" to cid.toString(),
            "qn" to "127",
            "fnval" to "4048",
            "fnver" to "0",
            "fourk" to "1",
        )
        // 高清（1080P+/4K/HDR/杜比）现在需要 WBI 签名；先走签名接口，
        // 失败再退回普通接口。反过来的话，普通接口有时只回 480P 但不报错，
        // 4K 就会被悄悄吞掉。
        var signedError: Throwable? = null
        var root = runCatching {
            rawRequest("/x/player/wbi/playurl", params, signed = true)
        }.onFailure { signedError = it }.getOrNull()
        val usedSigned = root != null
        if (root == null) {
            root = rawRequest("/x/player/playurl", params)
        }
        val code = root.optInt("code", 0)
        val dataValue = root.opt("data")
        val data = root.optJSONObject("data") ?: JSONObject()
        LogCollector.record(
            "API",
            "playurl signed=$usedSigned code=$code message=${root.optString("message")} " +
                "dataType=${dataValue?.javaClass?.simpleName ?: "null"} " +
                "loggedIn=$isLoggedIn mid=$mid sessLen=${cookieJar.value("SESSDATA").length} " +
                "buvidLen=${cookieJar.value("buvid3").length} wbi=${wbiKeys != null} " +
                "device=${android.os.Build.MANUFACTURER}/${android.os.Build.MODEL} " +
                "sdk=${android.os.Build.VERSION.SDK_INT} ${netInfo()} " +
                "err=${signedError?.javaClass?.simpleName}:${signedError?.message?.take(120)} " +
                describePlayData(data),
        )
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val parsed = parsePlayInfo(
            data,
            preferredQuality,
            detail,
            durationSec,
            codecPreference,
            cdnService,
        )
        LogCollector.record(
            "API",
            "playurl parsed qualities=${parsed.qualities.size} " +
                "audio=${parsed.audioUrl != null} fallback=${parsed.fallbackUrl != null} " +
                "accept=${parsed.acceptQuality}",
        )
        if (parsed.fallbackUrl == null && parsed.qualities.isNotEmpty()) {
            // DASH 解码失败时的兜底：优先 1080P，再 720P，最后才 480P。
            // 以前固定 480P，4K 解码一出错就整段变糊，看起来像“4K 没用”。
            var fallbackUrl = ""
            for (qn in listOf("80", "64", "32")) {
                val fallbackData = runCatching {
                    dataRequest(
                        "/x/player/playurl",
                        mapOf(
                            "bvid" to detail.bvid,
                            "cid" to cid.toString(),
                            "qn" to qn,
                            "fnval" to "1",
                            "fnver" to "0",
                        ),
                    )
                }.getOrNull() ?: continue
                val candidate = fallbackData.optJSONArray("durl")
                    ?.optJSONObject(0)
                    ?.s("url")
                    .orEmpty()
                if (candidate.isNotBlank()) {
                    fallbackUrl = candidate
                    break
                }
            }
            if (fallbackUrl.isNotBlank()) parsed.copy(fallbackUrl = fallbackUrl) else parsed
        } else {
            parsed
        }
    }

    suspend fun related(bvid: String): List<VideoItem> = withContext(Dispatchers.IO) {
        // 该接口的 data 是数组而不是对象，不能用 dataRequest
        val root = rawRequest("/x/web-interface/archive/related", mapOf("bvid" to bvid))
        if (root.optInt("code", 0) != 0) return@withContext emptyList()
        root.optJSONArray("data").objects().mapNotNull(::parseFeedVideo)
    }

    /**
     * 拉弹幕：与 BiliPai 一致走 protobuf 分段接口 seg.so，拿到 like / count 字段，
     * 这样热门弹幕条才能按「点赞数」显示 ×N，而不是本地统计相同文字。
     * 分段弹幕失败时退回旧的 XML 接口，保证至少还有弹幕可看。
     */
    suspend fun danmaku(cid: Long, durationSec: Int = 0): List<DanmakuItem> = withContext(Dispatchers.IO) {
        val segItems = runCatching { danmakuSegments(cid, durationSec) }.getOrDefault(emptyList())
        if (segItems.isNotEmpty()) return@withContext segItems
        danmakuXml(cid)
    }

    private suspend fun danmakuSegments(cid: Long, durationSec: Int): List<DanmakuItem> {
        if (cid <= 0L) return emptyList()
        // 每段 6 分钟；拿不到时长时先只取第一段。
        val segments = if (durationSec > 0) {
            ((durationSec + 359) / 360).coerceIn(1, 40)
        } else {
            1
        }
        val parsed = kotlinx.coroutines.coroutineScope {
            (1..segments).map { index ->
                async {
                    runCatching { fetchDanmakuSegment(cid, index) }.getOrNull()
                }
            }.awaitAll()
        }.filterNotNull()
        val result = ArrayList<DanmakuItem>(1024)
        for (bytes in parsed) {
            val elems = runCatching {
                com.android.purebilibili.danmaku.parser.DanmakuProto.parse(bytes)
            }.getOrDefault(emptyList())
            for (elem in elems) {
                val text = elem.content.trim()
                if (text.isBlank() || text.length > 90) continue
                if (elem.mode !in intArrayOf(1, 2, 3, 4, 5, 6, 7, 8)) continue
                result += DanmakuItem(
                    timeMs = elem.progress.toLong(),
                    mode = elem.mode,
                    color = elem.color,
                    size = if (elem.fontsize > 0) elem.fontsize.toFloat() else 25f,
                    text = text,
                    id = elem.id,
                    likeCount = elem.like,
                    count = elem.count,
                )
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private suspend fun fetchDanmakuSegment(cid: Long, index: Int): ByteArray? {
        val request = Request.Builder()
            .url(
                "https://api.bilibili.com/x/v2/dm/web/seg.so" +
                    "?type=1&oid=$cid&segment_index=$index",
            )
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        return client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) return null
            response.body?.bytes()?.takeIf { it.isNotEmpty() }
        }
    }

    private suspend fun danmakuXml(cid: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("$API/x/v1/dm/list.so?oid=$cid")
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        val bytes = client.newCall(request).awaitResponse().use { response ->
            if (!response.isSuccessful) return@withContext emptyList()
            response.body?.bytes() ?: return@withContext emptyList()
        }
        val xml = decodeDanmakuBytes(bytes)
        parseDanmakuXml(xml)
    }

    /**
     * 空降助手：从 BilibiliSponsorBlock 公共 API 拉取当前视频的片段。
     * 只在视频页进入时请求一次，结果缓存在内存里，不参与每帧逻辑。
     */
    suspend fun sponsorSegments(bvid: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
        if (bvid.isBlank()) return@withContext emptyList()
        SponsorSegmentCache.get(bvid)?.let { return@withContext it }
        val result = runCatching {
            val request = Request.Builder()
                .url("https://www.bsbsb.top/api/skipSegments?videoID=$bvid")
                .header("User-Agent", UA)
                .get()
                .build()
            val text = client.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                response.body?.string().orEmpty()
            }
            val array = JSONArray(text)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val segment = obj.optJSONArray("segment") ?: continue
                    if (segment.length() < 2) continue
                    val start = (segment.optDouble(0, 0.0) * 1000.0).toLong()
                    val end = (segment.optDouble(1, 0.0) * 1000.0).toLong()
                    if (end <= start) continue
                    add(
                        SponsorSegment(
                            startMs = start,
                            endMs = end,
                            category = obj.optString("category"),
                            actionType = obj.optString("actionType", "skip"),
                            uuid = obj.optString("UUID"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        SponsorSegmentCache.put(bvid, result)
        result
    }

    suspend fun comments(
        aid: Long,
        page: Int,
        upMid: Long = 0,
        sort: Int = 2,
    ): CommentPage = commentsByOid(aid.toString(), 1, page, upMid, sort)

    /**
     * 通用评论区：视频用 type=1 + aid，动态用 type=17 + id_str。
     * 两条路只是 oid/type 不同，解析和分页完全一致。
     */
    suspend fun commentsByOid(
        oid: String,
        type: Int,
        page: Int,
        upMid: Long = 0,
        sort: Int = 2,
    ): CommentPage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v2/reply",
            mapOf(
                "type" to type.toString(),
                "oid" to oid,
                "pn" to page.toString(),
                "ps" to "20",
                "sort" to sort.toString(),
            ),
        )
        val list = mutableListOf<CommentItem>()
        data.optJSONArray("top")?.objects()?.forEach { list += parseComment(it, upMid, true) }
        data.optJSONArray("replies")?.objects()?.forEach { list += parseComment(it, upMid, false) }
        val pageData = data.optJSONObject("page")
        val count = pageData?.optInt("count") ?: 0
        CommentPage(
            items = list.distinctBy { it.rpid },
            page = page,
            isEnd = page >= count,
            total = pageData?.optInt("acount") ?: 0,
        )
    }

    suspend fun replies(aid: Long, root: Long, page: Int = 1, upMid: Long = 0): CommentPage =
        repliesByOid(aid.toString(), 1, root, page, upMid)

    suspend fun repliesByOid(
        oid: String,
        type: Int,
        root: Long,
        page: Int = 1,
        upMid: Long = 0,
        sortTime: Boolean = false,
    ): CommentPage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v2/reply/reply",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "root" to root.toString(),
                "pn" to page.toString(),
                "ps" to "20",
                "nohot" to if (sortTime) "1" else "0",
            ),
        )
        val list = data.optJSONArray("replies").objects().map { parseComment(it, upMid, false) }
        val pageData = data.optJSONObject("page")
        val count = pageData?.optInt("count") ?: 0
        CommentPage(
            items = list.distinctBy { it.rpid },
            page = page,
            isEnd = page >= count,
            total = pageData?.optInt("acount") ?: 0,
        )
    }

    suspend fun addComment(
        aid: Long,
        message: String,
        root: Long = 0,
        parent: Long = 0,
        pictures: String? = null,
    ): Long? = addCommentByOid(aid.toString(), 1, message, root, parent, pictures)

    suspend fun addCommentByOid(
        oid: String,
        type: Int,
        message: String,
        root: Long = 0,
        parent: Long = 0,
        pictures: String? = null,
    ): Long? = withContext(Dispatchers.IO) {
        val params = mutableMapOf(
            "oid" to oid,
            "type" to type.toString(),
            "message" to message,
            "plat" to "1",
            "csrf" to csrf,
        )
        if (root > 0) params["root"] = root.toString()
        if (parent > 0) params["parent"] = parent.toString()
        if (!pictures.isNullOrBlank()) params["pictures"] = pictures
        val data = postRequest("/x/v2/reply/add", params, tolerateEmptyBody = false)
        data.l("rpid").takeIf { it > 0 }
            ?: data.optJSONObject("reply")?.l("rpid")?.takeIf { it > 0 }
            ?: 0L
    }

    /** 上传评论图片，返回可直接塞进 `pictures` 的 JSON；失败返回 null。 */
    suspend fun uploadCommentImage(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = appContext.contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
            val mime = resolver.getType(uri) ?: "image/jpeg"
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val width = bitmap?.width ?: 0
            val height = bitmap?.height ?: 0
            val body = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    "image",
                    bytes.toRequestBody(mime.toMediaTypeOrNull()),
                )
                .addFormDataPart("csrf", csrf)
                .build()
            val request = Request.Builder()
                .url("$API/x/dynamic/feed/dyn_img/upload")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .header("Origin", "https://www.bilibili.com")
                .post(body)
                .build()
            val json = client.newCall(request).awaitResponse().use { response ->
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) return@use null
                JSONObject(text)
            } ?: return@runCatching null
            if (json.optInt("code", 0) != 0) return@runCatching null
            val data = json.optJSONObject("data") ?: return@runCatching null
            val url = data.s("image_url").ifBlank { data.s("url") }
            if (url.isBlank()) return@runCatching null
            JSONObject().apply {
                put("img_src", url)
                put("img_width", width)
                put("img_height", height)
            }.toString()
        }.getOrNull()
    }

    /** 发送弹幕（videoup 的普通滚动弹幕）。 */
    suspend fun sendDanmaku(
        cid: Long,
        bvid: String,
        text: String,
        progressMs: Long = 0L,
        mode: Int = 1,
        fontSize: Int = 25,
        color: Int = 0xFFFFFF,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/dm/post",
            mapOf(
                "type" to "1",
                "oid" to cid.toString(),
                "bvid" to bvid,
                "msg" to text,
                "progress" to progressMs.coerceAtLeast(0L).toString(),
                "mode" to mode.toString(),
                "fontsize" to fontSize.toString(),
                "color" to color.toString(),
                "pool" to "0",
                "csrf" to csrf,
            ),
        )
        true
    }

    suspend fun likeComment(aid: Long, rpid: Long, like: Boolean): Boolean =
        likeCommentByOid(aid.toString(), 1, rpid, like)

    suspend fun likeCommentByOid(
        oid: String,
        type: Int,
        rpid: Long,
        like: Boolean,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/action",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "action" to if (like) "1" else "0",
                "csrf" to csrf,
            ),
        )
        true
    }

    suspend fun reportComment(aid: Long, rpid: Long, reason: Int = 1): Boolean =
        reportCommentByOid(aid.toString(), 1, rpid, reason)

    suspend fun reportCommentByOid(
        oid: String,
        type: Int,
        rpid: Long,
        reason: Int = 1,
    ): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/report",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "reason" to reason.toString(),
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 删除自己发出的评论。 */
    suspend fun deleteComment(aid: Long, rpid: Long): Boolean =
        deleteCommentByOid(aid.toString(), 1, rpid)

    suspend fun deleteCommentByOid(oid: String, type: Int, rpid: Long): Boolean =
        withContext(Dispatchers.IO) {
        postRequest(
            "/x/v2/reply/del",
            mapOf(
                "oid" to oid,
                "type" to type.toString(),
                "rpid" to rpid.toString(),
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 拉黑（加入黑名单）。 */
    suspend fun blockUser(mid: Long): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/relation/modify",
            mapOf(
                "fid" to mid.toString(),
                "act" to "5",
                "re_src" to "11",
                "csrf" to csrf,
            ),
        )
        true
    }

    /** 评论表情面板（需要登录；失败时返回空列表，界面回退到系统 emoji）。 */
    suspend fun replyEmotes(): List<EmoteItem> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/emote/user/panel/web",
                mapOf("business" to "reply"),
            )
            val out = mutableListOf<EmoteItem>()
            data.optJSONArray("packages")?.objects()?.forEach { pkg ->
                pkg.optJSONArray("emote")?.objects()?.forEach { emote ->
                    val text = emote.s("text").removeSurrounding("[", "]").trim()
                    val url = image(emote.s("url"), 120)
                    if (text.isNotBlank() && url.isNotBlank()) {
                        out += EmoteItem(text = text, url = url)
                    }
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    suspend fun likeVideo(aid: Long, like: Boolean): Boolean = withContext(Dispatchers.IO) {
        setVideoLike(aid, if (like) 1 else 2)
    }

    /** 点踩 / 取消点踩：like=3 踩，4 取消踩。 */
    suspend fun dislikeVideo(aid: Long, dislike: Boolean = true): Boolean =
        withContext(Dispatchers.IO) {
            setVideoLike(aid, if (dislike) 3 else 4)
        }

    /**
     * 点赞 / 点踩统一入口。
     * 走 web 的 `archive/like`：1 点赞、2 取消赞、3 点踩、4 取消踩。
     * 旧的 `/x/v2/view/like` 已下线（404 返回 HTML，导致“解析失败”），App 端
     * `/x/v2/view/like` 只认 access_key，用网页 cookie 会返回 -101，所以这里只用 web 接口。
     */
    private suspend fun setVideoLike(aid: Long, like: Int): Boolean {
        val root = rawRequest(
            "/x/web-interface/archive/like",
            mapOf("aid" to aid.toString(), "like" to like.toString(), "csrf" to csrf),
            method = "POST",
            tolerateEmptyBody = true,
        )
        val code = root.optInt("code", 0)
        return when (code) {
            // 重复点赞 / 重复点踩 / 未点赞过 / 未点踩过：目标状态已经达成，按成功处理
            0, 65004, 65005, 65006, 65007 -> true
            else -> throw BiliException(code, apiMessage(code, root.optString("message")))
        }
    }

    suspend fun coinVideo(aid: Long, count: Int, alsoLike: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            postRequest(
                "/x/web-interface/coin/add",
                mapOf(
                    "aid" to aid.toString(),
                    "multiply" to count.coerceIn(1, 2).toString(),
                    "select_like" to if (alsoLike) "1" else "0",
                    "csrf" to csrf,
                ),
            )
            true
        }

    suspend fun triple(aid: Long): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/web-interface/archive/like/triple",
            mapOf("aid" to aid.toString(), "csrf" to csrf),
        )
        true
    }

    /** 该视频已收藏在哪些收藏夹里（用于判断取消）。接口异常时返回空集合。 */
    suspend fun favoriteFolderIds(aid: Long): Set<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val root = rawRequest(
                "/x/v3/fav/resource/ids",
                mapOf("rid" to aid.toString(), "type" to "2"),
            )
            if (root.optInt("code", 0) != 0) return@runCatching emptySet()
            val data = root.optJSONArray("data") ?: root.optJSONObject("data")?.optJSONArray("list")
            val out = mutableSetOf<Long>()
            data?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.opt(i)
                    when (item) {
                        is Number -> out.add(item.toLong())
                        is JSONObject -> {
                            val id = item.l("id").takeIf { it > 0 } ?: item.l("media_id")
                            if (id > 0) out.add(id)
                        }
                    }
                }
            }
            out
        }.getOrDefault(emptySet())
    }

    /** 添加到收藏夹 / 从收藏夹移除。 */
    suspend fun setFavorite(aid: Long, folderId: Long, add: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            postRequest(
                "/x/v3/fav/resource/deal",
                mapOf(
                    "rid" to aid.toString(),
                    "type" to "2",
                    "add_media_ids" to if (add) folderId.toString() else "",
                    "del_media_ids" to if (add) "" else folderId.toString(),
                    "csrf" to csrf,
                    "platform" to "web",
                ),
            )
            true
        }

    /** 一次性从多个收藏夹移除（逗号分隔），比逐个调用更稳。 */
    suspend fun removeFavoriteAll(aid: Long, folderIds: Collection<Long>): Boolean =
        withContext(Dispatchers.IO) {
            if (folderIds.isEmpty()) return@withContext false
            postRequest(
                "/x/v3/fav/resource/deal",
                mapOf(
                    "rid" to aid.toString(),
                    "type" to "2",
                    "add_media_ids" to "",
                    "del_media_ids" to folderIds.joinToString(","),
                    "csrf" to csrf,
                    "platform" to "web",
                ),
            )
            true
        }

    /** 添加到稍后再看。 */
    suspend fun addWatchLater(aid: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            postRequest(
                "/x/v2/history/toview/add",
                mapOf("aid" to aid.toString(), "csrf" to csrf),
            )
        }.isSuccess
    }

    /** 删除单条历史记录：kid 的官方格式是 archive_{aid}，不是 bvid。 */
    suspend fun deleteHistory(aid: Long): Boolean = withContext(Dispatchers.IO) {
        if (aid <= 0) return@withContext false
        runCatching {
            postRequest(
                "/x/v2/history/delete",
                mapOf("kid" to "archive_$aid", "csrf" to csrf),
            )
        }.isSuccess
    }

    /** 从稍后再看移除：接口要 aid。 */
    suspend fun deleteWatchLater(aid: Long): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            postRequest(
                "/x/v2/history/toview/del",
                mapOf("aid" to aid.toString(), "csrf" to csrf),
            )
        }.isSuccess
    }

    suspend fun favoriteVideo(aid: Long, folderIds: String): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/v3/fav/resource/deal",
            mapOf(
                "rid" to aid.toString(),
                "type" to "2",
                "add_media_ids" to folderIds,
                "del_media_ids" to "",
                "csrf" to csrf,
                "platform" to "web",
            ),
        )
        true
    }

    suspend fun followUser(mid: Long, follow: Boolean): Boolean = withContext(Dispatchers.IO) {
        postRequest(
            "/x/relation/modify",
            mapOf(
                "fid" to mid.toString(),
                "act" to if (follow) "1" else "2",
                "re_src" to "11",
                "csrf" to csrf,
            ),
        )
        true
    }

    /**
     * 当前登录用户与 [mid] 的关系。返回 attribute：
     * 0 未关注、2 已关注、6 互关、128 拉黑；未登录或失败返回 null。
     */
    suspend fun relation(mid: Long): Int? = withContext(Dispatchers.IO) {
        if (!isLoggedIn || mid <= 0) return@withContext null
        runCatching {
            dataRequest("/x/relation", mapOf("fid" to mid.toString()))
                .optInt("attribute", 0)
        }.getOrNull()
    }

    /** HyperPiliPlus FollowHttp / FanHttp: server paging and owner privacy are preserved. */
    suspend fun relationUsers(mid: Long, fans: Boolean, page: Int = 1, frequent: Boolean = false): RelationPage =
        withContext(Dispatchers.IO) {
            require(mid > 0 && page > 0)
            val data = dataRequest(if (fans) "/x/relation/fans" else "/x/relation/followings",
                relationListParams(mid, fans, page, frequent))
            val raw = data.optJSONArray("list").objects()
            val ids = raw.map { it.l("mid") }.filter { it > 0 }.distinct()
            // 'attribute' in another person's following list describes THEIR relationship.
            val relations = if (isLoggedIn && ids.isNotEmpty()) runCatching {
                dataRequest("/x/relation/relations", mapOf("fids" to ids.joinToString(",")))
            }.getOrElse { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                null
            } else null
            val items = raw.mapNotNull { item ->
                val id = item.l("mid").takeIf { it > 0 } ?: return@mapNotNull null
                RelationUser(id, item.s("uname"), avatar(item.s("face"), 160), item.s("sign"),
                    if (!isLoggedIn) 0 else relations?.optJSONObject(id.toString())?.optInt("attribute", 0))
            }
            val total = data.optInt("total", 0)
            RelationPage(items, total, relationNextPage(page, raw.size, total))
        }

    // ---------------------------------------------------------------- account

    suspend fun history(): List<HistoryItem> = historyPage(0L, 0L, "").items

    suspend fun historyPage(
        maxCursor: Long,
        viewAtCursor: Long,
        businessCursor: String = "",
    ): HistoryPage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/web-interface/history/cursor",
            mapOf(
                "max" to maxCursor.toString(),
                "view_at" to viewAtCursor.toString(),
                "business" to businessCursor,
                "ps" to "20",
            ),
        )
        val items = data.optJSONArray("list").objects().mapNotNull { item ->
            val history = item.optJSONObject("history") ?: return@mapNotNull null
            val bvid = history.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            val stat = item.optJSONObject("stat") ?: item.optJSONObject("videos")
            HistoryItem(
                bvid = bvid,
                title = item.s("title"),
                cover = image(item.s("cover"), 420),
                author = item.s("author_name"),
                viewedAt = item.l("view_at"),
                progress = item.optInt("progress", 0),
                duration = item.optInt("duration", 0),
                play = stat?.optInt("view", 0) ?: 0,
                danmaku = stat?.optInt("danmaku", 0) ?: 0,
                aid = history.l("oid"),
            )
        }
        val cursor = data.optJSONObject("cursor")
        val cursorHasMore = cursor?.optBoolean("has_more", false) == true ||
            cursor?.optInt("has_more", 0) == 1 ||
            data.optBoolean("has_more", false)
        LogCollector.record(
            "HISTORY",
            "page max=$maxCursor viewAt=$viewAtCursor items=${items.size} " +
                "cursorMax=${cursor?.l("max")} cursorViewAt=${cursor?.l("view_at")} " +
                "cursorHasMore=${cursor?.opt("has_more")}",
        )
        HistoryPage(
            items = items,
            max = cursor?.l("max") ?: 0L,
            viewAt = cursor?.l("view_at") ?: items.lastOrNull()?.viewedAt ?: 0L,
            business = cursor?.s("business").orEmpty(),
            hasMore = cursorHasMore || items.size >= 30,
        )
    }

    /**
     * 云端观看进度：/x/player/v2 会返回登录账号在该视频上的 last_play_time。
     * 这是 B 站自己的续播数据，换设备/重装后仍然有效。
     * last_play_cid 和当前 cid 不一致时说明上次看的是别的分 P，不套用。
     */
    suspend fun cloudResumePositionMs(
        aid: Long,
        cid: Long,
        durationSec: Int = 0,
    ): Long = withContext(Dispatchers.IO) {
        if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext 0L
        val data = runCatching {
            dataRequest(
                "/x/player/v2",
                mapOf("aid" to aid.toString(), "cid" to cid.toString()),
            )
        }.getOrNull() ?: return@withContext 0L
        val lastCid = data.l("last_play_cid")
        if (lastCid > 0 && lastCid != cid) return@withContext 0L
        val raw = data.l("last_play_time")
        if (raw <= 0) return@withContext 0L
        // 接口有的版本给秒、有的给毫秒：按视频时长判断一下，避免差 1000 倍。
        val ms = if (durationSec > 0 && raw <= durationSec * 2L) raw * 1000L else raw
        ms.coerceAtLeast(0L)
    }

    /**
     * 上报观看进度，官方「历史记录」才会收录本 App 播放的视频。
     * 对应网页端的 heartbeat 接口：type=3（视频）、dt=2（web）。
     */
    suspend fun reportHistory(
        aid: Long,
        cid: Long,
        progressSec: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        if (aid <= 0 || cid <= 0 || !isLoggedIn) return@withContext false
        runCatching {
            postRequest(
                "/x/click-interface/web/heartbeat",
                mapOf(
                    "aid" to aid.toString(),
                    "cid" to cid.toString(),
                    "mid" to mid.toString(),
                    "csrf" to csrf,
                    "played_time" to progressSec.coerceAtLeast(0).toString(),
                    "real_played_time" to progressSec.coerceAtLeast(0).toString(),
                    "realtime" to progressSec.coerceAtLeast(0).toString(),
                    "start_ts" to (System.currentTimeMillis() / 1000).toString(),
                    "type" to "3",
                    "sub_type" to "0",
                    "dt" to "2",
                    "play_type" to "0",
                ),
            )
            true
        }.getOrDefault(false)
    }

    /**
     * 历史记录接口本身不带播放量/弹幕数，这里按 bvid 并发补取并缓存。
     * 并发压到 3，避免触发风控；同一 bvid 只请求一次。
     */
    suspend fun enrichHistoryStats(items: List<HistoryItem>): List<HistoryItem> {
        if (items.isEmpty()) return items
        val missing = items.map { it.bvid }.distinct().filter { cachedVideoStat(it) == null }
        if (missing.isNotEmpty()) {
            val limiter = Semaphore(6)
            kotlinx.coroutines.coroutineScope {
                missing.forEach { bvid ->
                    launch {
                        limiter.withPermit {
                            runCatching {
                                val root = rawRequest(
                                    "/x/web-interface/view",
                                    mapOf("bvid" to bvid),
                                )
                                val stat = root.optJSONObject("data")?.optJSONObject("stat")
                                if (stat != null) {
                                    putVideoStat(bvid, stat.l("view") to stat.l("danmaku"))
                                }
                            }
                        }
                    }
                }
            }
        }
        return items.map { item ->
            val cached = cachedVideoStat(item.bvid)
            if (cached == null) {
                item
            } else {
                item.copy(play = cached.first.toInt(), danmaku = cached.second.toInt())
            }
        }
    }

    /** 稍后再看：/x/v2/history/toview，字段结构与历史记录接近。 */
    suspend fun watchLater(): List<HistoryItem> = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/v2/history/toview", emptyMap())
        data.optJSONArray("list").objects().mapNotNull { item ->
            val bvid = item.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            val stat = item.optJSONObject("stat")
            HistoryItem(
                bvid = bvid,
                title = item.s("title"),
                cover = image(item.s("pic"), 420),
                author = item.optJSONObject("owner")?.s("name").orEmpty(),
                viewedAt = item.l("add_at"),
                progress = item.optInt("progress", 0),
                duration = item.optInt("duration", 0),
                play = stat?.optInt("view", 0) ?: 0,
                danmaku = stat?.optInt("danmaku", 0) ?: 0,
                aid = item.l("aid"),
            )
        }
    }

    suspend fun favoriteFolders(): List<FavoriteFolder> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/v3/fav/folder/created/list-all",
            mapOf("up_mid" to mid.toString()),
        )
        data.optJSONArray("list").objects().map { folder ->
            FavoriteFolder(
                id = folder.l("id"),
                title = folder.s("title"),
                count = folder.optInt("media_count", 0),
                cover = image(folder.s("cover"), 300),
            )
        }
    }

    suspend fun favoriteVideos(folderId: Long): List<VideoItem> =
        favoriteVideosPage(folderId, 1).first

    suspend fun favoriteVideosPage(folderId: Long, page: Int): Pair<List<VideoItem>, Boolean> =
        withContext(Dispatchers.IO) {
            val data = dataRequest(
                "/x/v3/fav/resource/list",
                mapOf(
                    "media_id" to folderId.toString(),
                    "pn" to page.toString(),
                    "ps" to "20",
                    "order" to "mtime",
                    "type" to "0",
                    "tid" to "0",
                    "platform" to "web",
                ),
            )
            val medias = data.optJSONArray("medias").objects()
            val videos = medias.mapNotNull { media ->
                val bvid = media.s("bvid")
                if (bvid.isBlank()) return@mapNotNull null
                val upper = media.optJSONObject("upper")
                val cnt = media.optJSONObject("cnt_info")
                VideoItem(
                    bvid = bvid,
                    aid = media.l("id"),
                    title = media.s("title"),
                    cover = image(media.s("cover"), 420),
                    ownerName = upper?.s("name").orEmpty(),
                    ownerFace = "",
                    ownerMid = upper?.l("mid") ?: 0,
                    play = cnt?.l("play") ?: 0,
                    danmaku = cnt?.l("danmaku") ?: 0,
                    duration = media.optInt("duration", 0),
                    pubdate = media.l("pubtime"),
                )
            }
            videos to data.optBoolean("has_more", medias.size >= 40)
        }

    suspend fun userDynamics(mid: Long, offset: String = ""): DynamicPage =
        withContext(Dispatchers.IO) {
            val data = dataRequest(
                "/x/polymer/web-dynamic/v1/feed/space",
                mapOf(
                    "host_mid" to mid.toString(),
                    "offset" to offset,
                    "page" to "1",
                    "web_location" to "333.999",
                    "features" to "itemOpusStyle,listOnlyfans,opusBigCover,onlyfansVote",
                ),
                referer = "https://space.bilibili.com/$mid/dynamic",
            )
            DynamicPage(
                items = data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) },
                offset = data.s("offset"),
                hasMore = data.optBoolean("has_more", false),
            )
        }

    /** UP 主投稿视频数。 */
    suspend fun upVideoCount(mid: Long): Long = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
            data.l("video")
        }.getOrDefault(0L)
    }

    /** 当前同时观看人数：返回 (数字, 展示文案)。 */
    suspend fun onlineTotal(aid: Long, cid: Long): Pair<Long, String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = dataRequest(
                    "/x/player/online/total",
                    mapOf("aid" to aid.toString(), "cid" to cid.toString()),
                )
                data.optLong("count", 0L) to data.s("total")
            }.getOrDefault(0L to "")
        }

    suspend fun relationStat(mid: Long): Pair<Long, Long> = withContext(Dispatchers.IO) {
        val data = dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
        (data.l("following") to data.l("follower"))
    }

    /** 用户获赞总数（官方空间统计）。 */
    suspend fun userLikes(mid: Long): Long = withContext(Dispatchers.IO) {
        runCatching {
            dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
        }.getOrDefault(0L)
    }

    suspend fun followings(): List<FollowedUser> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/relation/followings",
            mapOf(
                "vmid" to mid.toString(),
                "pn" to "1",
                "ps" to "50",
                "order" to "desc",
                "order_type" to "attention",
            ),
        )
        data.optJSONArray("list").objects().map { user ->
            FollowedUser(
                mid = user.l("mid"),
                name = user.s("uname"),
                face = avatar(user.s("face"), 160),
                sign = user.s("sign"),
            )
        }
    }

    /** 视频字幕列表：`/x/player/v2` 的 `subtitle.subtitles`（含 AI 字幕）。 */
    suspend fun subtitles(aid: Long, cid: Long): List<SubtitleTrack> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/player/v2",
                mapOf("aid" to aid.toString(), "cid" to cid.toString()),
            )
            val subtitle = data.optJSONObject("subtitle") ?: return@runCatching emptyList()
            subtitle.optJSONArray("subtitles").objects().mapNotNull { item ->
                val url = item.s("subtitle_url")
                    .ifBlank { item.s("subtitle_url_v2") }
                if (url.isBlank()) return@mapNotNull null
                SubtitleTrack(
                    id = item.s("id").ifBlank { item.s("lan") },
                    label = item.s("lan_doc").ifBlank { item.s("lan") },
                    url = if (url.startsWith("//")) "https:$url" else url,
                    isAi = item.optInt("ai_type", 0) > 0,
                )
            }
        }.getOrDefault(emptyList())
    }

    /** 下载并解析字幕 JSON（B 站字幕是 `body[]` 的 from/to/content）。 */
    suspend fun subtitleCues(url: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url(if (url.startsWith("//")) "https:$url" else url)
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            val text = client.newCall(request).awaitResponse().use { response ->
                if (!response.isSuccessful) return@runCatching emptyList()
                response.body?.string().orEmpty()
            }
            val root = JSONObject(text)
            root.optJSONArray("body").objects().mapNotNull { cue ->
                val content = cue.s("content").trim()
                if (content.isBlank()) return@mapNotNull null
                val from = (cue.optDouble("from", 0.0) * 1000).toLong()
                val to = (cue.optDouble("to", 0.0) * 1000).toLong()
                SubtitleCue(from, to.coerceAtLeast(from + 300L), content)
            }
        }.getOrDefault(emptyList())
    }

    suspend fun dynamics(): List<DynamicItem> = dynamicPage("all").items

    /** Hyper-PiliPlus uses the same feed for categories and a selected UP. */
    override suspend fun dynamicPage(type: String, offset: String, hostMid: Long?): DynamicPage =
        withContext(Dispatchers.IO) {
            val data = try {
                dataRequest(DynamicFeedContract.feedPath, DynamicFeedContract.feedParams(type, offset, hostMid))
            } catch (error: BiliException) {
                if (error.code == 4101132) return@withContext DynamicPage(emptyList(), "", false)
                throw error
            }
            DynamicPage(
                items = data.optJSONArray("items").objects().mapNotNull { parseDynamicItem(it) },
                offset = data.s("offset"),
                hasMore = data.optBoolean("has_more", false),
            )
        }

    override suspend fun dynamicPortal(): DynamicPortal = withContext(Dispatchers.IO) {
        val data = dataRequest(DynamicFeedContract.portalPath, DynamicFeedContract.portalParams)
        val live = data.optJSONObject("live_users")
        DynamicPortal(
            upList = parseDynamicUpPage(data.optJSONObject("up_list")),
            liveUsers = live?.optJSONArray("items").objects().mapNotNull {
                val roomId = it.l("room_id")
                if (roomId <= 0) null else DynamicLiveUser(
                    mid = it.l("mid"), name = it.s("uname"), face = avatar(it.s("face"), 120),
                    roomId = roomId, title = it.s("title"),
                )
            },
            liveCount = live?.optInt("count", 0) ?: 0,
        )
    }

    override suspend fun dynamicUpList(offset: String): DynamicUpPage = withContext(Dispatchers.IO) {
        parseDynamicUpPage(dataRequest(DynamicFeedContract.upListPath, DynamicFeedContract.upListParams(offset)))
    }

    private fun parseDynamicUpPage(data: JSONObject?): DynamicUpPage = DynamicUpPage(
        items = data?.optJSONArray("items").objects().mapNotNull {
            val mid = it.l("mid")
            if (mid <= 0) null else DynamicAuthor(mid, it.s("uname"), avatar(it.s("face"), 120), it.optBoolean("has_update"))
        }.distinctBy { it.mid },
        offset = data?.s("offset").orEmpty(),
        hasMore = data?.optBoolean("has_more", false) ?: false,
    )

    private fun parseDynamicItem(item: JSONObject, allowOrig: Boolean = true): DynamicItem? {
        val modules = item.optJSONObject("modules") ?: return null
        val author = modules.optJSONObject("module_author") ?: return null
        val dynamic = modules.optJSONObject("module_dynamic")
        val desc = dynamic?.optJSONObject("desc")?.s("text").orEmpty()
        val major = dynamic?.optJSONObject("major")
        val archive = major?.optJSONObject("archive")
        val opus = major?.optJSONObject("opus")
        val article = major?.optJSONObject("article")
        val common = major?.optJSONObject("common")
        val pgc = major?.optJSONObject("pgc")
        val live = major?.optJSONObject("live")
        val liveRcmd = major?.optJSONObject("live_rcmd")
            ?: major?.optJSONObject("subscription_new")?.optJSONObject("live_rcmd")
        val liveInfo = liveRcmd?.s("content")?.takeIf { it.isNotBlank() }
            ?.let { runCatching { JSONObject(it).optJSONObject("live_play_info") }.getOrNull() }
        val liveRecommendation = liveInfo?.let {
            DynamicLiveRecommendation(
                roomId = it.l("room_id"), title = it.s("title"), cover = image(it.s("cover"), 720),
                live = it.optInt("live_status") == 1, areaName = it.s("area_name"),
                watched = it.optJSONObject("watched_show")?.s("text_large").orEmpty(),
            )
        }
        val medialist = major?.optJSONObject("medialist")
        val courses = major?.optJSONObject("courses")
        val music = major?.optJSONObject("music")
        val ugcSeason = major?.optJSONObject("ugc_season")
        val video = archive?.let {
            VideoItem(
                bvid = it.s("bvid"),
                aid = it.l("aid"),
                title = it.s("title"),
                cover = image(it.s("cover"), 480),
                ownerName = author.s("name"),
                ownerFace = avatar(author.s("face"), 120),
                ownerMid = author.l("mid"),
                play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
                danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
                duration = parseDurationText(it.s("duration_text")),
                pubdate = 0,
                cid = 0,
            )
        } ?: ugcSeason?.let {
            VideoItem(
                bvid = it.s("bvid"),
                aid = it.l("aid"),
                title = it.s("title"),
                cover = image(it.s("cover"), 480),
                ownerName = author.s("name"),
                ownerFace = avatar(author.s("face"), 120),
                ownerMid = author.l("mid"),
                play = parseCountText(it.optJSONObject("stat")?.optString("play").orEmpty()),
                danmaku = parseCountText(it.optJSONObject("stat")?.optString("danmaku").orEmpty()),
                duration = parseDurationText(it.s("duration_text")),
                pubdate = 0,
                cid = 0,
            )
        }
        // 图文 opus：feed 在 itemOpusStyle 下把标题 / 正文放在 major.opus 里，
        // 只读 module_dynamic.desc 会出现“很多内容显示不出来”。
        val opusSummary = opus?.optJSONObject("summary")?.s("text").orEmpty()
        val title = opus?.s("title").orEmpty()
            .ifBlank { article?.s("title").orEmpty() }
            .ifBlank { common?.s("title").orEmpty() }
            .ifBlank { live?.s("title").orEmpty() }
            .ifBlank { medialist?.s("title").orEmpty() }
            .ifBlank { courses?.s("title").orEmpty() }
            .ifBlank { music?.s("title").orEmpty() }
        val text = desc
            .ifBlank { opusSummary }
            .ifBlank { article?.s("desc").orEmpty() }
            .ifBlank { common?.s("desc").orEmpty() }
            .ifBlank { common?.s("summary").orEmpty() }
            .ifBlank { pgc?.s("desc").orEmpty() }
            .ifBlank { medialist?.s("desc").orEmpty() }
            .ifBlank { music?.s("desc").orEmpty() }
        val images = buildList {
            major?.optJSONObject("draw")?.optJSONArray("items")?.objects()?.forEach {
                it.s("src").takeIf(String::isNotBlank)?.let { src -> add(image(src, 720)) }
            }
            opus?.optJSONArray("pics")?.objects()?.forEach {
                it.s("url").takeIf(String::isNotBlank)?.let { url -> add(image(url, 720)) }
            }
            article?.optJSONArray("covers")?.let { covers ->
                for (i in 0 until covers.length()) {
                    val value = covers.opt(i)
                    val url = when (value) {
                        is String -> value
                        is JSONObject -> value.s("url")
                        else -> ""
                    }
                    if (url.isNotBlank()) add(image(url, 720))
                }
            }
            common?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            live?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            medialist?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            courses?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
            music?.s("cover").orEmpty().takeIf { it.isNotBlank() }?.let { add(image(it, 720)) }
        }.filter { it.isNotBlank() }.distinct()
        val stat = modules.optJSONObject("module_stat")
        val basic = item.optJSONObject("basic")
        val orig = if (allowOrig) {
            item.optJSONObject("orig")?.let { parseDynamicItem(it, allowOrig = false) }
        } else {
            null
        }
        return DynamicItem(
            id = item.s("id_str"),
            name = author.s("name"),
            face = avatar(author.s("face"), 120),
            mid = author.l("mid"),
            time = author.s("pub_time"),
            text = text,
            images = images,
            video = video,
            like = stat?.optJSONObject("like")?.optInt("count", 0) ?: 0,
            comment = stat?.optJSONObject("comment")?.optInt("count", 0) ?: 0,
            forward = stat?.optJSONObject("forward")?.optInt("count", 0) ?: 0,
            liked = stat?.optJSONObject("like")?.optBoolean("status", false) ?: false,
            commentOid = basic?.s("comment_id_str").orEmpty()
                .ifBlank { item.s("id_str") },
            commentType = (basic?.optInt("comment_type", 17) ?: 17)
                .takeIf { it > 0 } ?: 17,
            title = title,
            orig = orig,
            authorVip = author.optJSONObject("vip")?.optInt("vipType", 0) == 2,
            isCollection = !ugcSeason?.s("bvid").isNullOrBlank(),
            pgcCover = pgc?.s("cover").orEmpty().takeIf { it.isNotBlank() }
                ?.let { image(it, 720) }.orEmpty(),
            pgcTitle = pgc?.s("title").orEmpty(),
            pgcLabel = when (pgc?.optInt("sub_type", 0)) {
                3 -> "纪录片"
                1 -> "番剧"
                2 -> "电影"
                4 -> "国创"
                5 -> "电视剧"
                else -> "剧集"
            },
            moduleTag = modules.optJSONObject("module_tag")?.s("text").orEmpty(),
            liveRecommendation = liveRecommendation,
            ipLocation = author.s("pub_location_text")
                .ifBlank { author.s("pub_location") }
                .ifBlank { author.s("ip_location") }
                .ifBlank { author.s("location") },
        )
    }

    /**
     * 动态详情官方接口：feed 只给摘要时，打开详情用 detail 接口拿完整的
     * opus 正文 / 图片 / 转发内容。失败返回 null，由调用方回退到列表项。
     */
    suspend fun dynamicDetail(id: String): DynamicItem? = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/polymer/web-dynamic/v1/detail",
                mapOf(
                    "id" to id,
                    "dynamic_id" to id,
                    "platform" to "web",
                    "gaia_source" to "main_web",
                    "web_location" to "333.1368",
                    "features" to "itemOpusStyle,opusBigCover,onlyfansVote," +
                        "endFooterHidden,decorationCard,onlyfansAssetsV2,ugcDelete," +
                        "onlyfansQaCard,commentsNewVersion",
                ),
            )
            data.optJSONObject("item")?.let { parseDynamicItem(it) }
        }.getOrNull()
    }

    /** 动态点赞 / 取消。新接口要求 csrf 放 URL、正文是 JSON（form 会 4100001）。 */
    suspend fun likeDynamic(id: String, like: Boolean): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            ensureBuvid()
            val jsonBody = JSONObject()
                .put("dyn_id_str", id)
                .put("up", if (like) 1 else 2)
                .toString()
            val request = Request.Builder()
                .url("$API/x/dynamic/feed/dyn/thumb?csrf=$csrf")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .header("Origin", "https://www.bilibili.com")
                .header("Content-Type", "application/json")
                .post(jsonBody.toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            client.newCall(request).awaitResponse().use { response ->
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) throw BiliException(-1, "服务器返回为空")
                val json = JSONObject(body)
                val code = json.optInt("code", 0)
                if (code != 0) {
                    throw BiliException(code, apiMessage(code, json.optString("message")))
                }
            }
            true
        }.onFailure {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("BiliApi", "likeDynamic failed id=$id like=$like", it)
            }
        }.getOrDefault(false)
    }

    /** 动态详情评论（type=17）。 */
    suspend fun dynamicComments(id: String, page: Int = 1): CommentPage =
        withContext(Dispatchers.IO) {
            val data = dataRequest(
                "/x/v2/reply",
                mapOf(
                    "type" to "17",
                    "oid" to id,
                    "pn" to page.toString(),
                    "ps" to "20",
                    "sort" to "2",
                ),
            )
            val list = mutableListOf<CommentItem>()
            data.optJSONArray("top")?.objects()?.forEach { list += parseComment(it, 0, true) }
            data.optJSONArray("replies")?.objects()?.forEach { list += parseComment(it, 0, false) }
            val pageData = data.optJSONObject("page")
            val count = pageData?.optInt("count") ?: 0
            CommentPage(
                items = list.distinctBy { it.rpid },
                page = page,
                isEnd = page >= count,
                total = pageData?.optInt("acount") ?: 0,
            )
        }

    suspend fun sendDynamicComment(id: String, message: String): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                postRequest(
                    "/x/v2/reply/add",
                    mapOf(
                        "oid" to id,
                        "type" to "17",
                        "message" to message,
                        "plat" to "1",
                        "csrf" to csrf,
                    ),
                )
                true
            }.getOrDefault(false)
        }

    /**
     * App 聚合接口 /x/v2/space。
     * 头图、充电人数、IP 属地都在这一份返回里，避免为每个字段各发一次请求。
     */
    private suspend fun spaceAppPayload(mid: Long): JSONObject? = withContext(Dispatchers.IO) {
        runCatching {
            // app.bilibili.com 不吃网页 WBI 签名，必须按 App 规则加 appkey/ts/sign，
            // 否则只会返回 -400「请求错误」，头图永远空白。这里和 PiliPlus 的请求一致。
            val params = mutableMapOf(
                "build" to "8430300",
                "version" to "8.43.0",
                "mobi_app" to "android",
                "platform" to "android",
                "channel" to "master",
                "vmid" to mid.toString(),
                "c_locale" to "zh_CN",
                "s_locale" to "zh_CN",
                "statistics" to "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
            )
            signAppParams(params)
            dataRequest(
                "/x/v2/space",
                params,
                base = APP_API,
                userAgent = APP_UA,
                referer = "https://space.bilibili.com/$mid",
                headers = APP_HEADERS,
            )
        }.getOrNull()
    }

    private fun spaceBannerCandidates(payload: JSONObject?, info: JSONObject): List<String> {
        val images = payload?.optJSONObject("images")
        val first = images?.optJSONObject("collection_top_simple")?.optJSONObject("top")
            ?.optJSONArray("result")?.optJSONObject(0)
        val collection = first?.optJSONObject("item")?.optJSONObject("image")?.s("default_image").orEmpty()
        val web = info.s("top_photo").ifBlank { info.optJSONObject("card")?.s("top_photo").orEmpty() }
            .ifBlank { info.optJSONObject("space")?.s("top_photo").orEmpty() }
        // The App endpoint can return its default pink dressing even when a custom web banner exists.
        return orderedSpaceBanners(collection, first?.s("cover").orEmpty(), web,
            images?.s("imgUrl").orEmpty(), images?.s("night_imgurl").orEmpty()).map(::image)
    }

    private fun spaceBannerAlignment(payload: JSONObject?): Float {
        val item = payload?.optJSONObject("images")?.optJSONObject("collection_top_simple")
            ?.optJSONObject("top")?.optJSONArray("result")?.optJSONObject(0)?.optJSONObject("item")
        val image = item?.optJSONObject("image") ?: item?.optJSONObject("animation")
        return spaceBannerAlignment(image?.s("location").orEmpty(), image?.optDouble("height", 0.0) ?: 0.0)
    }

    /** 为 TA 充电的人数：使用 PiliPlus 同款 App 聚合接口的 elec.total。 */
    suspend fun spaceChargeCount(mid: Long): Long = withContext(Dispatchers.IO) {
        val payload = spaceAppPayload(mid) ?: return@withContext 0L
        val elec = payload.optJSONObject("elec")
        elec?.l("total")?.takeIf { it > 0L }
            ?: elec?.optJSONArray("list")?.length()?.toLong()?.takeIf { it > 0L }
            ?: 0L
    }

    suspend fun space(mid: Long): SpaceInfo = withContext(Dispatchers.IO) {
        // 资料、关系、投稿、导航数互不依赖，并行拉取；页面总耗时从“四次相加”变成“最慢的一次”。
        val infoDeferred = async {
            dataRequest(
                "/x/space/wbi/acc/info",
                mapOf(
                    "mid" to mid.toString(),
                    "web_location" to "1550101",
                    "dm_img_list" to "[]",
                    "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                    "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                    "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
        }
        val statDeferred = async {
            runCatching {
                dataRequest("/x/relation/stat", mapOf("vmid" to mid.toString()))
            }.getOrNull()
        }
        val videosDeferred = async {
            runCatching {
                dataRequest(
                    "/x/space/wbi/arc/search",
                    mapOf(
                        "mid" to mid.toString(),
                        "pn" to "1",
                        "ps" to "30",
                        "order" to "pubdate",
                        "web_location" to "1550101",
                        "dm_img_list" to "[]",
                        "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                        "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                        "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
                    ),
                    signed = true,
                    referer = "https://space.bilibili.com/$mid",
                )
            }.getOrNull()
        }
        val navDeferred = async {
            runCatching {
                dataRequest("/x/space/navnum", mapOf("mid" to mid.toString()))
            }.getOrNull()
        }
        // 头图、充电人数、IP 属地共用这一份 App 聚合数据。
        val appSpaceDeferred = async { spaceAppPayload(mid) }

        val info = infoDeferred.await()
        val stat = statDeferred.await()
        val videos = videosDeferred.await()
        val nav = navDeferred.await()
        val appSpace = appSpaceDeferred.await()
        val medal = info.optJSONObject("fans_medal")?.optJSONObject("medal")
        val official = info.optJSONObject("official")
        // 和 PiliPlus 一样只认 App 聚合接口的 card.vip。
        // PiliPlus 的 Vip.fromJson 是 status 优先、vipStatus 兜底，type 优先、vipType 兜底。
        val cardVip = appSpace?.optJSONObject("card")?.optJSONObject("vip")
        val vipStatus = cardVip?.optInt("status", 0)
            ?.takeIf { it > 0 }
            ?: (cardVip?.optInt("vipStatus", 0) ?: 0)
        val vipLabelObj = cardVip?.optJSONObject("label")
        val vipTheme = vipLabelObj?.s("label_theme").orEmpty()
        val vipType = cardVip?.optInt("type", 0)
            ?.takeIf { it > 0 }
            ?: (cardVip?.optInt("vipType", 0) ?: 0)
        val vipLabel = vipLabelObj?.s("text").orEmpty().ifBlank {
            when {
                vipTheme.contains("hundred", ignoreCase = true) -> "百年大会员"
                vipTheme.contains("ten_year", ignoreCase = true) -> "十年大会员"
                vipTheme.contains("ten", ignoreCase = true) -> "十年大会员"
                vipTheme.contains("annual", ignoreCase = true) -> "年度大会员"
                vipType == 2 -> "年度大会员"
                vipStatus == 1 -> "大会员"
                else -> ""
            }
        }
        val elec = appSpace?.optJSONObject("elec")
        val charge = elec?.l("total")?.takeIf { it > 0L }
            ?: elec?.optJSONArray("list")?.length()?.toLong()?.takeIf { it > 0L }
            ?: 0L
        val vlist = videos?.optJSONObject("list")?.optJSONArray("vlist").objects().orEmpty()
        SpaceInfo(
            profile = UserProfile(
                mid = info.l("mid"),
                name = info.s("name"),
                face = avatar(info.s("face"), 200),
                sign = info.s("sign"),
                level = info.optInt("level", 0),
                coins = 0.0,
                vip = vipStatus == 1,
                vipLabel = vipLabel,
                follower = stat?.l("follower") ?: 0,
                following = stat?.l("following") ?: 0,
                likes = info.optLong("likes", 0L).takeIf { it > 0L }
                    ?: runCatching {
                        dataRequest("/x/space/upstat", mapOf("mid" to mid.toString())).l("likes")
                    }.getOrDefault(0L).takeIf { it > 0L }
                    ?: nav?.l("likes") ?: 0L,
                banner = spaceBannerCandidates(appSpace, info).firstOrNull().orEmpty(),
                bannerFallbacks = spaceBannerCandidates(appSpace, info).drop(1),
                bannerAlignmentY = spaceBannerAlignment(appSpace),
                officialTitle = official?.s("title").orEmpty(),
                officialDesc = official?.s("desc").orEmpty(),
                officialType = official?.optInt("type", 0) ?: 0,
                fansMedalName = medal?.s("medal_name").orEmpty(),
                fansMedalLevel = medal?.optInt("level", 0) ?: 0,
                fansMedalColor = medal?.optInt("medal_color", 0) ?: 0,
                chargingCount = charge,
                levelExp = info.optLong("level_exp", 0L),
                nextLevelExp = info.optLong("next_level_exp", 0L),
            ),
            videos = vlist.mapNotNull(::parseSpaceVideo),
            videoCount = nav?.optInt("video", 0) ?: vlist.size,
            isFollowed = info.optBoolean("is_followed", false),
        )
    }

    suspend fun spaceSeasonsSeries(mid: Long): List<SpaceSeason> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/polymer/web-space/seasons_series_list",
                mapOf(
                    "mid" to mid.toString(),
                    "page_num" to "1",
                    "page_size" to "10",
                    "web_location" to "333.1387",
                ),
                referer = "https://space.bilibili.com/$mid",
            )
            val lists = data.optJSONObject("items_lists")
            val out = ArrayList<SpaceSeason>()
            lists?.optJSONArray("seasons_list")?.objects().orEmpty().forEach { item ->
                val meta = item.optJSONObject("meta") ?: return@forEach
                val id = meta.l("season_id")
                val title = meta.s("name").ifBlank { meta.s("title") }
                val cover = image(meta.s("cover"), 480)
                if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
                out += SpaceSeason(
                    id = id,
                    title = title,
                    cover = cover,
                    total = meta.optInt("total", 0),
                    isSeries = false,
                )
            }
            lists?.optJSONArray("series_list")?.objects().orEmpty().forEach { item ->
                val meta = item.optJSONObject("meta") ?: return@forEach
                val id = meta.l("series_id")
                val title = meta.s("name").ifBlank { meta.s("title") }
                val cover = image(meta.s("cover"), 480)
                if (id <= 0L || title.isBlank() || cover.isBlank()) return@forEach
                out += SpaceSeason(
                    id = id,
                    title = title,
                    cover = cover,
                    total = meta.optInt("total", 0),
                    isSeries = true,
                )
            }
            out
        }.getOrDefault(emptyList())
    }

    /** 合集 / 系列的投稿列表。 */
    suspend fun spaceCollectionArchives(
        mid: Long,
        collection: SpaceSeason,
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        runCatching {
            val data = if (collection.isSeries) {
                dataRequest(
                    "/x/series/archives",
                    mapOf(
                        "mid" to mid.toString(),
                        "series_id" to collection.id.toString(),
                        "only_normal" to "true",
                        "sort" to "desc",
                        "pn" to "1",
                        "ps" to "30",
                        "web_location" to "333.1387",
                    ),
                    referer = "https://space.bilibili.com/$mid",
                )
            } else {
                dataRequest(
                    "/x/polymer/web-space/seasons_archives_list",
                    mapOf(
                        "mid" to mid.toString(),
                        "season_id" to collection.id.toString(),
                        "page_num" to "1",
                        "page_size" to "30",
                        "sort_reverse" to "false",
                        "web_location" to "333.1387",
                    ),
                    referer = "https://space.bilibili.com/$mid",
                )
            }
            data.optJSONArray("archives").objects().mapNotNull { archive ->
                val bvid = archive.s("bvid")
                if (bvid.isBlank()) return@mapNotNull null
                val stat = archive.optJSONObject("stat")
                VideoItem(
                    bvid = bvid,
                    aid = archive.l("aid"),
                    title = archive.s("title"),
                    cover = image(archive.s("pic"), 480),
                    ownerName = "",
                    ownerFace = "",
                    ownerMid = mid,
                    play = stat?.l("view") ?: archive.l("play"),
                    danmaku = stat?.l("danmaku") ?: 0L,
                    duration = archive.optInt("duration", 0),
                    pubdate = archive.l("pubdate"),
                    cid = archive.l("cid"),
                )
            }
        }.getOrDefault(emptyList())
    }

    /** 空间投稿（按发布时间，分页）。 */
    suspend fun spaceVideos(mid: Long, page: Int): List<VideoItem> = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/space/wbi/arc/search",
                mapOf(
                    "mid" to mid.toString(),
                    "pn" to page.toString(),
                    "ps" to "30",
                    "order" to "pubdate",
                    "web_location" to "1550101",
                    "dm_img_list" to "[]",
                    "dm_img_str" to "V2ViR0wgMS4wIChPcGVuR0wgRVMgMi4wIENocm9taXVtKQ",
                    "dm_cover_img_str" to "QU5HTEUgKE5WSURJQSwgTlZJRElBIEdlRm9yY2UgR1RYIDEwNjAgNkdCIERpcmVjdDNEMTEp",
                    "dm_img_inter" to "{\"ds\":[],\"wh\":[0,0,0],\"of\":[0,0,0]}",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
            data.optJSONObject("list")?.optJSONArray("vlist").objects()
                .mapNotNull(::parseSpaceVideo)
        }.getOrDefault(emptyList())
    }

    /** 空间图文投稿。 */
    suspend fun spaceArticles(mid: Long, page: Int): List<SpaceArticle> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = dataRequest(
                    "/x/space/wbi/article",
                    mapOf(
                        "mid" to mid.toString(),
                        "pn" to page.toString(),
                        "ps" to "20",
                        "sort" to "publish_time",
                    ),
                    signed = true,
                    referer = "https://space.bilibili.com/$mid",
                )
                data.optJSONArray("articles").objects().mapNotNull { article ->
                    val id = article.l("id")
                    if (id <= 0L) return@mapNotNull null
                    val covers = article.optJSONArray("covers")
                    val cover = when {
                        covers != null && covers.length() > 0 -> covers.optString(0)
                        else -> article.s("cover")
                    }
                    SpaceArticle(
                        id = id,
                        title = article.s("title"),
                        cover = image(cover, 480),
                        view = article.optJSONObject("stats")?.l("view")
                            ?: article.l("view"),
                        publishTime = article.l("publish_time"),
                    )
                }
            }.getOrDefault(emptyList())
        }

    /** 空间追番。 */
    suspend fun spaceBangumi(mid: Long, page: Int): List<SpaceBangumi> =
        withContext(Dispatchers.IO) {
            runCatching {
                val data = dataRequest(
                    "/x/space/bangumi",
                    mapOf(
                        "mid" to mid.toString(),
                        "pn" to page.toString(),
                        "ps" to "30",
                    ),
                    referer = "https://space.bilibili.com/$mid",
                )
                data.optJSONArray("list").objects().mapNotNull { item ->
                    val seasonId = item.l("season_id")
                    val title = item.s("title")
                    if (seasonId <= 0L || title.isBlank()) return@mapNotNull null
                    SpaceBangumi(
                        seasonId = seasonId,
                        title = title,
                        cover = image(item.s("cover"), 480),
                        progress = item.s("progress"),
                        total = item.optInt("total_count", 0),
                    )
                }
            }.getOrDefault(emptyList())
        }

    /** UP 主小店：mall.bilibili.com 的 App 签名 POST 接口，先取第一页 8 个。 */
    suspend fun spaceShop(mid: Long): List<SpaceShopItem> = withContext(Dispatchers.IO) {
        runCatching {
            val extras = mutableMapOf(
                "actionKey" to "appkey",
                "build" to "8430300",
                "mVersion" to "309",
                "mallVersion" to "8430300",
                "statistics" to
                    "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
            )
            signAppParams(extras)
            val query = extras.entries.joinToString("&") { (key, value) ->
                "${android.net.Uri.encode(key)}=${android.net.Uri.encode(value)}"
            }
            val body = JSONObject()
                .put("from", "cps_productTab_$mid")
                .put("searchAfter", 0)
                .put("msource", "cps_productTab_$mid")
                .put("pageSize", 8)
                .put("upMid", mid.toString())
                .toString()
            val request = Request.Builder()
                .url("https://mall.bilibili.com/community-hub/small_shop/feed/tab/item?$query")
                .post(body.toRequestBody("application/json".toMediaTypeOrNull()))
                .header("User-Agent", APP_UA)
                .header("Referer", "https://mall.bilibili.com/")
                .header("Origin", "https://mall.bilibili.com")
                .build()
            val root = executeOnce(request, tolerateEmptyBody = true)
            if (root.optInt("code", 0) != 0) return@runCatching emptyList()
            val data = root.optJSONObject("data") ?: return@runCatching emptyList()
            data.optJSONArray("data").objects().mapNotNull { item ->
                val title = item.s("title")
                if (title.isBlank()) return@mapNotNull null
                val coverObj = item.optJSONObject("cover")
                val priceObj = item.optJSONObject("netPrice")
                val price = if (priceObj != null) {
                    buildString {
                        priceObj.s("pricePrefix").takeIf { it.isNotBlank() }?.let {
                            append(it); append(' ')
                        }
                        append(priceObj.s("priceSymbol"))
                        append(priceObj.s("netPrice"))
                    }
                } else {
                    ""
                }
                val labels = item.optJSONArray("belowLabels").objects()
                    .mapNotNull { it.s("title").takeIf(String::isNotBlank) }
                    .joinToString("|")
                SpaceShopItem(
                    title = title,
                    cover = image(coverObj?.s("url").orEmpty(), 480),
                    price = price,
                    source = labels.ifBlank { item.s("itemSourceName") },
                    url = item.s("cardUrl"),
                )
            }
        }.getOrDefault(emptyList())
    }

    // ---------------------------------------------------------------- search

    /**
     * 搜索词联想：PiliPlus 同款 s.search.bilibili.com/main/suggest。
     * 返回 term 作为真正提交的词，name 里可能带高亮 HTML，这里清洗掉。
     */
    suspend fun searchSuggest(term: String): List<String> = withContext(Dispatchers.IO) {
        if (term.isBlank()) return@withContext emptyList()
        runCatching {
            val root = rawRequest(
                "/main/suggest",
                mapOf(
                    "term" to term,
                    "main_ver" to "v1",
                    "highlight" to term,
                ),
                base = "https://s.search.bilibili.com",
            )
            root.optJSONObject("result")
                ?.optJSONArray("tag")
                .objects()
                .mapNotNull { item ->
                    item.s("term")
                        .ifBlank { cleanHtml(item.s("name")) }
                        .takeIf { it.isNotBlank() }
                }
                .distinct()
                .take(8)
        }.getOrDefault(emptyList())
    }

    /** B 站热搜词。依次尝试「搜索发现」和「热搜词」，都失败时给一份兜底列表。 */
    suspend fun hotKeywords(): List<String> = withContext(Dispatchers.IO) {
        // 1) 搜索发现：data.trending.list[].keyword
        val square = runCatching {
            val data = dataRequest(
                "/x/web-interface/search/square",
                mapOf("limit" to "10"),
                signed = true,
            )
            val list = data.optJSONObject("trending")?.optJSONArray("list")
            val out = ArrayList<String>()
            if (list != null) {
                for (i in 0 until list.length()) {
                    val word = list.optJSONObject(i)?.optString("keyword").orEmpty()
                    if (word.isNotBlank()) out.add(word)
                }
            }
            out
        }.getOrDefault(emptyList())

        if (square.isNotEmpty()) return@withContext square

        // 2) 热搜词接口
        runCatching {
            val root = rawRequest(
                path = "/main/hotword",
                params = emptyMap(),
                base = "https://s.search.bilibili.com",
            )
            val array = root.optJSONArray("data") ?: return@runCatching emptyList<String>()
            val result = ArrayList<String>(array.length())
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val word = item.optString("keyword").ifBlank { item.optString("name") }
                if (word.isNotBlank()) result.add(word)
            }
            result
        }.getOrDefault(emptyList())
    }

    suspend fun search(
        keyword: String,
        page: Int = 1,
        /** totalrank 默认 / click 播放多 / pubdate 新发布 / dm 弹幕多 */
        order: String = "totalrank",
        /** 0 不限 / 1 0-10 分钟 / 2 10-30 / 3 30-60 / 4 60 以上 */
        duration: Int = 0,
        pubtimeBegin: Long? = null,
        pubtimeEnd: Long? = null,
    ): List<VideoItem> = withContext(Dispatchers.IO) {
        val params = LinkedHashMap<String, String>().apply {
            put("search_type", "video")
            put("keyword", keyword)
            put("page", page.toString())
            put("page_size", "20")
            put("order", order)
            if (duration in 1..4) put("duration", duration.toString())
            pubtimeBegin?.let { put("pubtime_begin_s", it.toString()) }
            pubtimeEnd?.let { put("pubtime_end_s", it.toString()) }
        }
        val data = dataRequest(
            "/x/web-interface/wbi/search/type",
            params,
            signed = true,
        )
        data.optJSONArray("result").objects().mapNotNull { item ->
            val bvid = item.s("bvid")
            if (bvid.isBlank()) return@mapNotNull null
            VideoItem(
                bvid = bvid,
                aid = item.l("aid"),
                title = cleanHtml(item.s("title")),
                cover = image(item.s("pic"), 420),
                ownerName = cleanHtml(item.s("author")),
                ownerFace = "",
                ownerMid = item.l("mid"),
                play = item.s("play").toLongOrNull() ?: 0,
                danmaku = item.s("danmaku").toLongOrNull() ?: 0,
                duration = parseDurationText(item.s("duration")),
                pubdate = item.l("pubdate"),
            )
        }
    }

    /** 搜索用户（综合页 / 用户 Tab）。 */
    suspend fun searchUsers(keyword: String, page: Int = 1): List<SearchUser> =
        withContext(Dispatchers.IO) {
            val data = dataRequest(
                "/x/web-interface/wbi/search/type",
                mapOf(
                    "search_type" to "bili_user",
                    "keyword" to keyword,
                    "page" to page.toString(),
                    "page_size" to "20",
                ),
                signed = true,
            )
            data.optJSONArray("result").objects().mapNotNull { item ->
                val mid = item.l("mid")
                if (mid <= 0L) return@mapNotNull null
                SearchUser(
                    mid = mid,
                    name = cleanHtml(item.s("uname")),
                    face = image(item.s("upic"), 160),
                    sign = cleanHtml(item.s("usign")),
                    fans = item.s("fans").toLongOrNull() ?: 0,
                    videos = item.s("videos").toLongOrNull() ?: 0,
                    level = item.optInt("level", 0),
                )
            }
        }

    suspend fun hotSearch(): List<String> = withContext(Dispatchers.IO) {
        val data = runCatching {
            dataRequest("/x/web-interface/search/square", mapOf("limit" to "10"))
        }.getOrNull() ?: return@withContext emptyList()
        data.optJSONObject("trending")?.optJSONArray("list").objects()
            ?.mapNotNull { it.s("keyword").takeIf(String::isNotBlank) }
            .orEmpty()
    }

    suspend fun messages(type: Int, page: Int = 1): List<MessageItem> = messagesPage(type).items

    suspend fun messagesPage(type: Int, cursor: MessageCursor? = null): MessageFeedPage = withContext(Dispatchers.IO) {
        if (type == 3) return@withContext systemMessages(cursor)
        val path = when (type) {
            0 -> "/x/msgfeed/reply"
            1 -> "/x/msgfeed/at"
            2 -> "/x/msgfeed/like"
            else -> error("Unknown message feed")
        }
        val params = messageFeedParams(type, cursor)
        val data = dataRequest(path, params)
        // 收到的赞返回的是 { latest: { items }, total: { items } } 两个分桶，
        // 不是顶层的 items；原来只读顶层，所以这个页面一直是空的。
        val rawItems = if (type == 2) {
            val merged = ArrayList<JSONObject>()
            data.optJSONObject("latest")?.optJSONArray("items").objects().let(merged::addAll)
            data.optJSONObject("total")?.optJSONArray("items").objects().let(merged::addAll)
            merged.distinctBy { it.s("id") }
        } else {
            data.optJSONArray("items").objects()
        }
        val parsed = rawItems.mapNotNull { item ->
            val detail = item.optJSONObject("item")
            // 收到的赞把用户放在 users 数组里，其余类型是单个 user 对象
            val users = item.optJSONArray("users").objects()
            val user = item.optJSONObject("user") ?: users.firstOrNull()
            val names = users.mapNotNull {
                it.s("nickname").ifBlank { it.s("uname") }.takeIf(String::isNotBlank)
            }
            val detailType = detail?.s("type").orEmpty()
            val isLike = type == 2
            val isAt = type == 1 || detailType == "at"
            val sourceContent = detail?.s("source_content").orEmpty()
            val targetContent = detail?.s("target_reply_content").orEmpty()
            val rootContent = detail?.s("root_reply_content").orEmpty()
            val businessId = detail?.optInt("business_id", 0) ?: 0
            val rawContent = item.s("content")
                .ifBlank { detail?.s("content").orEmpty() }
                .ifBlank { detail?.s("message").orEmpty() }
            // 回复通知的正文经常只放在 source_content 里；点赞通知优先保留原始正文，
            // 这样「收到喜欢」右侧才能显示被点赞的评论原文。
            val content = if (isLike) rawContent else rawContent.ifBlank { sourceContent }
            val likeVideo = isLike && (
                detailType == "video" || detailType == "archive" ||
                    (businessId == 1 && sourceContent.isBlank() &&
                        targetContent.isBlank() && rootContent.isBlank() && rawContent.isBlank())
                )
            val commentOnMyVideo = !isLike && !isAt &&
                (detailType == "video" ||
                    businessId == 1 && targetContent.isBlank() && rootContent.isBlank())
            val action = when {
                isLike && likeVideo -> "赞了我的视频"
                isLike -> "赞了我的评论"
                isAt -> "提到了我"
                commentOnMyVideo -> "对我的视频发表了评论"
                else -> "回复了我的评论"
            }
            // 右侧：层主评论 > 我原来的评论；评论我视频时固定用视频封面。
            val subjectText = when {
                commentOnMyVideo -> ""
                isLike -> if (likeVideo) "" else sourceContent
                    .ifBlank { targetContent }
                    .ifBlank { rootContent }
                    .ifBlank { rawContent }
                rootContent.isNotBlank() -> rootContent
                targetContent.isNotBlank() -> targetContent
                else -> ""
            }
            // 底部引用只在线程里出现：我在这层楼里说过的话。
            val quote = if (!isLike && rootContent.isNotBlank()) targetContent else ""
            val source = sourceContent
                .ifBlank { rootContent }
                .ifBlank { targetContent }
                .ifBlank { rawContent }
                .ifBlank { detail?.s("desc").orEmpty() }
                .ifBlank { item.s("title") }
            val title = detail?.s("title").orEmpty().ifBlank { item.s("title") }
            val subjectCover = if (likeVideo || commentOnMyVideo) {
                detail?.s("cover").orEmpty()
                    .ifBlank { detail?.s("pic").orEmpty() }
            } else {
                ""
            }
            val videoSubject = likeVideo || commentOnMyVideo
            val subjectId = (detail?.l("subject_id") ?: 0L)
                .takeIf { it > 0L }
                ?: item.l("subject_id")
            val nativeUri = detail?.s("native_uri").orEmpty()
                .ifBlank { detail?.s("uri").orEmpty() }
                .ifBlank { item.s("native_uri").orEmpty() }
            // 消息接口最可靠的是 native_uri：bilibili://video/{aid}/?comment_root_id=..&comment_secondary_id=..
            // 之前只看 bvid / subject_id，回复消息里这两项经常为空或是别的东西，导致跳过去是空的。
            val uriAid = listOf(
                "bilibili://video/(\\d+)",
                "bilibili://video/[aA][vV](\\d+)",
                "bilibili\\.com/video/[aA][vV](\\d+)",
            ).firstNotNullOfOrNull { pattern ->
                Regex(pattern).find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull()
            }
            val uriBvid = Regex("BV[0-9A-Za-z]{10}").find(nativeUri)?.value.orEmpty()
            val uriRootRpid = Regex("[?&]comment_root_id=(\\d+)")
                .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val uriSecondRpid = Regex("[?&]comment_secondary_id=(\\d+)")
                .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            val bvid = uriBvid.ifBlank {
                detail?.s("bvid").orEmpty()
            }
            val aid = when {
                uriAid != null && uriAid > 0L -> uriAid
                subjectId > 0L -> subjectId
                else -> (detail?.l("oid") ?: 0L).takeIf { it > 0L }
                    ?: (item.l("oid") ?: 0L)
            }
            val seekSeconds = Regex("[?&#](?:t|start|time)=([0-9]+)")
                .find(nativeUri)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
            // 点赞通知的文字字段经常是空的，只要有用户就保留，否则整页会「没有内容」。
            if (type != 2 && title.isBlank() && content.isBlank() && source.isBlank()) {
                return@mapNotNull null
            }
            MessageItem(
                id = item.s("id").ifBlank { "${type}_${item.l("id")}" },
                title = title,
                content = content,
                time = item.l("reply_time")
                    .takeIf { it > 0 }
                    ?: item.l("ctime").takeIf { it > 0 }
                    ?: item.l("timestamp").takeIf { it > 0 }
                    ?: item.l("like_time").takeIf { it > 0 }
                    ?: (item.s("pub_time").toLongOrNull() ?: 0L),
                name = user?.s("nickname").orEmpty().ifBlank { user?.s("uname").orEmpty() },
                avatar = avatar(user?.s("avatar").orEmpty(), 96),
                bvid = bvid,
                action = action,
                source = source,
                userMid = user?.l("mid") ?: 0L,
                followed = isFollowedUser(user, item),
                likeNames = names,
                likeCount = maxOf(
                    item.optInt("counts", 0),
                    detail?.optInt("counts", 0) ?: 0,
                    names.size,
                ),
                aid = aid,
                rootRpid = uriRootRpid.takeIf { it > 0L } ?: (detail?.l("root_id") ?: 0L),
                targetRpid = uriSecondRpid.takeIf { it > 0L } ?: (detail?.l("target_id") ?: 0L),
                sourceRpid = uriSecondRpid.takeIf { it > 0L }
                    ?: (detail?.l("source_id") ?: 0L),
                businessId = businessId,
                seekMs = seekSeconds * 1000L,
                nativeUri = nativeUri,
                noticeState = item.optInt("notice_state", 0),
                quote = quote,
                subjectText = subjectText,
                subjectCover = subjectCover,
                videoSubject = videoSubject,
                atMe = isAt,
                commentOnMyVideo = commentOnMyVideo,
            )
        }
        // 补充封面 / UP 主关系：同一个视频只查一次，失败不影响文字消息。
        val enriched = enrichMessageVideos(parsed, type)
        LogCollector.record(
            "MSG",
            "type=$type items=${rawItems.size} parsed=${enriched.size}",
        )
        val next = (if (type == 2) data.optJSONObject("total") else data)?.optJSONObject("cursor")
        val nextId = next?.l("id") ?: 0L
        val nextTime = next?.l("time") ?: 0L
        MessageFeedPage(enriched, messageContinuation(cursor, nextId, nextTime,
            next == null || next.optBoolean("is_end", false)))
    }

    /**
     * 消息接口只给文字和视频 id，不给封面、也不说回复者是不是 UP 主。
     * 这里按 bvid 去重补一次视频信息；每页最多补 12 个，失败就保留纯文字卡片。
     */
    private suspend fun enrichMessageVideos(
        items: List<MessageItem>,
        type: Int,
    ): List<MessageItem> {
        if (items.isEmpty() || type == 1) return items
        fun resolvedBvid(item: MessageItem): String =
            item.bvid.ifBlank { aidToBvid(item.aid) }
        val targets = items
            .filter { resolvedBvid(it).isNotBlank() && (type == 0 || it.videoSubject) }
            .distinctBy { resolvedBvid(it) }
            .take(12)
        if (targets.isEmpty()) return items
        val briefs = withContext(Dispatchers.IO) {
            kotlinx.coroutines.coroutineScope {
                targets.map { item ->
                    async {
                        val bvid = resolvedBvid(item)
                        val cached = synchronized(messageVideoCache) { messageVideoCache[bvid] }
                        val brief = cached ?: runCatching {
                            val detail = view(bvid)
                            MessageVideoBrief(detail.cover, detail.ownerMid).also {
                                synchronized(messageVideoCache) {
                                    messageVideoCache[bvid] = it
                                }
                            }
                        }.getOrNull()
                        bvid to brief
                    }
                }.awaitAll()
            }
        }.filter { it.second != null }.associate { it.first to it.second!! }
        if (briefs.isEmpty()) return items
        return items.map { item ->
            val bvid = resolvedBvid(item)
            val brief = briefs[bvid] ?: return@map item
            item.copy(
                bvid = item.bvid.ifBlank { bvid },
                subjectCover = if (item.videoSubject && item.subjectCover.isBlank()) {
                    brief.cover
                } else {
                    item.subjectCover
                },
                isUp = item.isUp ||
                    (item.userMid > 0L && brief.ownerMid > 0L && item.userMid == brief.ownerMid),
            )
        }
    }

    /**
     * 关注关系在不同接口里字段名不一样：`follow` / `is_follow` / `follow_status` / `attribute`。
     * 少读一个就会出现「我关注的人」永远为空。
     */
    private fun isFollowedUser(user: JSONObject?, item: JSONObject): Boolean {
        fun followed(obj: JSONObject?): Boolean {
            if (obj == null) return false
            if (obj.optBoolean("follow", false)) return true
            if (obj.optBoolean("is_follow", false)) return true
            if (obj.optInt("is_follow", 0) > 0) return true
            if (obj.optInt("follow_status", 0) > 0) return true
            val attribute = obj.optInt("attribute", 0)
            // 1=悄悄关注 / 2=已关注 / 6=互关，其余（拉黑等）不算。
            return attribute == 1 || attribute == 2 || attribute == 6
        }
        return followed(user) || followed(item)
    }

    /** 消息中心会话列表：一次 25 条，按 session_ts 继续往后翻。 */
    suspend fun messageSessions(
        followingOnly: Boolean,
        sessionTs: Long = 0L,
        pageSize: Int = 25,
        onEnriched: ((List<MessageSession>) -> Unit)? = null,
    ): MessageSessionPage = withContext(Dispatchers.IO) {
        val request = mutableMapOf("session_type" to "4", "group_fold" to "0",
            "unfollow_fold" to "0", "sort_rule" to "2", "size" to pageSize.toString(), "mobi_app" to "web")
        if (sessionTs > 0L) request["session_ts"] = sessionTs.toString()
        val data = dataRequest("/session_svr/v1/session_svr/get_sessions", request, base = VC)
        val allSessions = data.optJSONArray("session_list").objects()
        val raw = if (followingOnly) allSessions.filter { it.optInt("is_follow") == 1 } else allSessions
        val system = raw.map { it.toMessageSession() }
        // 首屏先返回：内存缓存里已经有的名字/头像立即带上，其余留给后台补全。
        val quick = system.map { session ->
            val cached = MessageUserCache.get(session.talkerId)
            if (cached != null) {
                session.copy(name = cached.first, avatar = cached.second)
            } else {
                session
            }
        }
        val needResolve = quick
            .filter { it.name.isBlank() && it.sessionType == 1 && !it.isSystem }
        val resolved = if (needResolve.isEmpty()) quick else kotlinx.coroutines.coroutineScope {
            val limit = kotlinx.coroutines.sync.Semaphore(4)
            val names = needResolve.map { session -> async {
                limit.acquire()
                try {
                    userBrief(session.talkerId)?.let { brief ->
                        MessageUserCache.put(session.talkerId, brief.first, brief.second)
                        session.id to brief
                    }
                } finally { limit.release() }
            } }.awaitAll().filterNotNull().toMap()
            quick.map { session -> names[session.id]?.let { session.copy(name = it.first, avatar = it.second) } ?: session }
        }
        val nextTs = allSessions.lastOrNull()?.l("session_ts") ?: 0L
        val more = if (data.has("has_more")) data.optInt("has_more") == 1 else allSessions.size >= pageSize
        MessageSessionPage(resolved, sessionContinuation(sessionTs, nextTs, more))
    }

    /** 拉取一个私信会话的消息。 */
    suspend fun privateMessages(talkerId: Long, sessionType: Int = 1, size: Int = 30): List<ChatMessage> =
        privateMessagePage(talkerId, sessionType, size).items

    suspend fun privateMessagePage(
        talkerId: Long,
        sessionType: Int = 1,
        size: Int = 30,
        before: Long? = null,
    ): ChatMessagePage = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/svr_sync/v1/svr_sync/fetch_session_msgs",
            mapOf(
                "talker_id" to talkerId.toString(),
                "session_type" to sessionType.toString(),
                "size" to size.toString(),
                "sender_device_id" to "1",
                "build" to "0",
                "mobi_app" to "web",
                "web_location" to "333.1296",
            ) + (before?.let { mapOf("begin_seqno" to it.toString()) } ?: emptyMap()),
            signed = true,
            base = VC,
        )
        val own = mid
        val items = data.optJSONArray("messages").objects().mapNotNull { message ->
            val type = message.optInt("msg_type", 1)
            val raw = message.s("content")
            val content = runCatching { JSONObject(raw) }.getOrNull()
            val withdrawn = message.optInt("msg_status") == 1
            val text = if (withdrawn) "消息已撤回" else when (type) {
                1 -> content?.s("content").orEmpty()
                2 -> ""
                6, 7 -> content?.s("title").orEmpty()
                else -> content?.s("content").orEmpty().ifBlank { raw }
            }
            val image = if (type == 2 && !withdrawn) content?.s("url").orEmpty() else ""
            if (text.isBlank() && image.isBlank() && type != 1) return@mapNotNull null
            ChatMessage(
                id = message.l("msg_key").takeIf { it != 0L }
                    ?: message.l("msg_seqno"),
                sequence = message.l("msg_seqno"),
                senderId = message.l("sender_uid"),
                timestamp = message.l("timestamp").takeIf { it > 0L }
                    ?: message.l("msg_time"),
                text = text,
                imageUrl = image,
                isMine = message.l("sender_uid") == own,
            )
        }.sortedBy { it.sequence }
        val next = data.optString("min_seqno").toLongOrNull() ?: 0L
        ChatMessagePage(items, sessionContinuation(before ?: 0L, next, data.optInt("has_more") == 1))
    }

    /** 发送一条纯文本私信。 */
    suspend fun sendPrivateMessage(talkerId: Long, text: String, sessionType: Int = 1,
        imageContent: String? = null): Boolean =
        withContext(Dispatchers.IO) {
            if ((text.isBlank() && imageContent == null) || csrf.isBlank()) return@withContext false
            val now = System.currentTimeMillis() / 1000L
            val content = imageContent ?: JSONObject().put("content", text).toString()
            val params = mapOf(
                "msg[sender_uid]" to mid.toString(),
                "msg[receiver_id]" to talkerId.toString(),
                "msg[receiver_type]" to sessionType.toString(),
                "msg[msg_type]" to if (imageContent != null) "2" else "1",
                "msg[msg_status]" to "0",
                "msg[dev_id]" to java.util.UUID.randomUUID().toString(),
                "msg[timestamp]" to now.toString(),
                "msg[new_face_version]" to "1",
                "msg[content]" to content,
                "from_firework" to "0",
                "build" to "0",
                "mobi_app" to "web",
                "csrf" to csrf,
                "csrf_token" to csrf,
            )
            val root = runCatching {
                rawRequest(
                    "/web_im/v1/web_im/send_msg",
                    params,
                    base = VC,
                    method = "POST",
                    tolerateEmptyBody = false,
                )
            }.getOrNull() ?: return@withContext false
            root.optInt("code", 0) == 0
        }

    /** 官方「新增粉丝」：/x/relation/fans。 */
    suspend fun newFollowers(page: Int = 1): List<MessageItem> = withContext(Dispatchers.IO) {
        val data = dataRequest(
            "/x/relation/fans",
            mapOf(
                "vmid" to mid.toString(),
                "pn" to page.toString(),
                "ps" to "20",
                "order" to "desc",
                "order_type" to "attention",
            ),
        )
        data.optJSONArray("list").objects().mapNotNull { fan ->
            val fanMid = fan.l("mid")
            if (fanMid <= 0L) return@mapNotNull null
            val attribute = fan.optInt("attribute", 0)
            MessageItem(
                id = "fans_$fanMid",
                title = "",
                content = fan.s("sign"),
                time = fan.l("mtime").takeIf { it > 0L } ?: fan.l("ctime"),
                name = fan.s("uname"),
                avatar = avatar(fan.s("face"), 120),
                action = "关注了我",
                source = fan.s("sign"),
                userMid = fanMid,
                // 粉丝列表接口里 attribute 2/6 表示我已经关注 / 互关，用来决定「回关」按钮。
                followed = attribute == 2 || attribute == 6 ||
                    fan.optBoolean("is_follow", false),
            )
        }
    }

    /** 仅取用户昵称和头像，供私信会话补全。 */
    suspend fun userBrief(mid: Long): Pair<String, String>? = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/space/wbi/acc/info",
                mapOf("mid" to mid.toString()),
                signed = true,
            )
            val name = data.s("name")
            if (name.isBlank()) null else name to avatar(data.s("face"), 120)
        }.getOrNull()
    }

    suspend fun messageFeedUnread(): Map<String, Int> {
        val data = dataRequest("/x/msgfeed/unread", mapOf("mobi_app" to "web", "build" to "0"))
        return listOf("reply", "at", "like", "sys_msg").associateWith { data.optInt(it).coerceAtLeast(0) }
    }

    suspend fun messageUnreadCount(): Int = withContext(Dispatchers.IO) {
        val params = mapOf("build" to "0", "mobi_app" to "web", "web_location" to "333.1365")
        val privateData = dataRequest("/session_svr/v1/session_svr/single_unread",
            params + ("unread_type" to "0"), base = "https://api.vc.bilibili.com")
        val feed = dataRequest("/x/msgfeed/unread", params)
        (listOf("follow_unread", "unfollow_unread").sumOf { privateData.optInt(it).coerceAtLeast(0) } +
            listOf("at", "like", "reply", "sys_msg").sumOf { feed.optInt(it).coerceAtLeast(0) })
    }

    /** Only report success after the server confirms that every unread category is clear. */
    suspend fun clearAllUnread(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
        val all = sessions.associateBy { it.id }.toMutableMap()
        var cursor = 0L
        do {
            val page = messageSessions(false, cursor)
            page.items.forEach { all[it.id] = it }
            cursor = page.next ?: 0L
        } while (cursor != 0L)
        markSessionsRead(all.values.toList())
        // These feeds acknowledge their current notifications when opened, as in HyperPiliPlus.
        for (type in 0..3) messagesPage(type)
        messageUnreadCount() == 0
    }

    /** 仅取用户 IP 属地，供动态详情在接口未返回发布 IP 时兜底。 */
    suspend fun userIpLocation(mid: Long): String = withContext(Dispatchers.IO) {
        runCatching {
            val data = dataRequest(
                "/x/space/wbi/acc/info",
                mapOf(
                    "mid" to mid.toString(),
                    "web_location" to "1550101",
                ),
                signed = true,
                referer = "https://space.bilibili.com/$mid",
            )
            data.s("ip_location")
        }.getOrDefault("")
    }

    suspend fun markSessionsRead(sessions: List<MessageSession>): Boolean = withContext(Dispatchers.IO) {
        acknowledgeUnreadSessions(sessions) { session ->
            dataRequest("/session_svr/v1/session_svr/update_ack", sessionParams(session) +
                ("ack_seqno" to session.maxSeqno.toString()), base = VC, signed = true)
        }
        true
    }

    private fun sessionParams(session: MessageSession) = mapOf(
        "talker_id" to session.talkerId.toString(), "session_type" to session.sessionType.toString(),
        "csrf" to csrf, "csrf_token" to csrf, "mobi_app" to "web", "build" to "0")

    suspend fun removeMessageSession(session: MessageSession) {
        dataRequest("/session_svr/v1/session_svr/remove_session", sessionParams(session),
            base = VC, signed = true, method = "POST")
    }

    suspend fun pinMessageSession(session: MessageSession, pinned: Boolean) {
        dataRequest("/session_svr/v1/session_svr/set_top", sessionParams(session) +
            ("op_type" to sessionPinOperation(pinned)), base = VC, signed = true, method = "POST")
    }

    suspend fun muteMessageSession(session: MessageSession, muted: Boolean) {
        dataRequest("/link_setting/v1/link_setting/set_msg_dnd", mapOf(
            "uid" to mid.toString(), "dnd_uid" to session.talkerId.toString(),
            "setting" to if (muted) "1" else "0", "csrf" to csrf, "mobi_app" to "web", "build" to "0"),
            base = VC, method = "POST")
    }

    suspend fun deleteFeedMessage(type: Int, id: String) {
        require(type in 0..2)
        postRequest("/x/msgfeed/del", mapOf("tp" to messageDeleteType(type).toString(), "id" to id,
            "csrf" to csrf, "csrf_token" to csrf, "mobi_app" to "web", "build" to "0"), tolerateEmptyBody = false)
    }

    suspend fun setLikeNotice(id: String, enabled: Boolean) {
        postRequest("/x/msgfeed/notice", mapOf("tp" to "0", "id" to id,
            "notice_state" to if (enabled) "0" else "1", "platform" to "web", "mobi_app" to "web",
            "build" to "0", "csrf" to csrf), tolerateEmptyBody = false)
    }

    private suspend fun systemMessages(cursor: MessageCursor?): MessageFeedPage {
        val params = mutableMapOf("page_size" to "20", "mobi_app" to "web", "build" to "0")
        cursor?.let { params["cursor"] = it.id.toString() }
        val root = rawRequest("/x/sys-msg/query_notify_list", params, base = "https://message.bilibili.com")
        val code = root.optInt("code")
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        val items = root.optJSONArray("data").objects().map {
            val raw = it.s("content")
            val content = runCatching { JSONObject(raw).optString("web", raw) }.getOrDefault(raw)
            MessageItem(id = it.s("id"), title = it.s("title"), content = content,
                time = 0L, name = "系统通知", avatar = "", source = it.s("time_at"), systemCursor = it.l("cursor"))
        }
        if (cursor == null && items.isNotEmpty()) {
            try {
                dataRequest("/x/sys-msg/update_cursor", mapOf("cursor" to items.first().systemCursor.toString(),
                    "csrf" to csrf), base = "https://message.bilibili.com")
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { LogCollector.record("MSG", "system read acknowledgement failed") }
        }
        val next = items.lastOrNull()?.systemCursor ?: 0L
        return MessageFeedPage(items, messageContinuation(cursor, next, 0L, items.size < 20))
    }

    suspend fun deleteSystemMessage(id: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("csrf", csrf).put("ids", JSONArray().put(id.toLong()))
            .put("station_ids", JSONArray()).put("type", 4).put("mobi_app", "android")
        val request = Request.Builder()
            .url("https://message.bilibili.com/x/sys-msg/del_notify_list?mobi_app=android&csrf=${encode(csrf)}")
            .header("User-Agent", UA).header("Referer", "https://message.bilibili.com/")
            .post(body.toString().toRequestBody("application/json".toMediaTypeOrNull())).build()
        val response = executeOnce(request, false)
        val code = response.optInt("code")
        if (code != 0) throw BiliException(code, apiMessage(code, response.optString("message")))
    }

    private fun JSONObject.toMessageSession(): MessageSession {
        val type = optInt("session_type", 1)
        val systemType = optInt("system_msg_type", 0)
        val account = optJSONObject("account_info")
        val name = account?.s("name").orEmpty()
            .ifBlank { if (type == 2) s("group_name") else "" }
        val avatar = account?.s("pic_url").orEmpty()
            .ifBlank { if (type == 2) s("group_cover") else "" }
        val last = optJSONObject("last_msg")
        return MessageSession(
            id = "${type}_${l("talker_id")}",
            talkerId = l("talker_id"),
            sessionType = type,
            name = name,
            avatar = avatar,
            preview = last?.let { previewOfMessage(it) }.orEmpty(),
            time = (l("session_ts") / 1_000_000L).takeIf { it > 0L }
                ?: (last?.l("timestamp") ?: 0L),
            unread = optInt("unread_count", 0),
            isSystem = systemType != 0,
            followed = optInt("is_follow") == 1,
            pinned = l("top_ts") > 0L,
            muted = optInt("is_dnd") == 1,
            maxSeqno = l("max_seqno"),
            sessionTs = l("session_ts"),
        )
    }

    private fun previewOfMessage(message: JSONObject): String {
        val type = message.optInt("msg_type", 0)
        val raw = message.optString("content")
        val obj = runCatching { JSONObject(raw) }.getOrNull()
        return when (type) {
            2 -> "[图片]"
            306 -> obj?.s("content").orEmpty()
            10, 11, 12 -> obj?.s("title").orEmpty()
                .ifBlank { obj?.s("text").orEmpty() }
                .ifBlank { obj?.s("desc").orEmpty() }
            else -> obj?.s("content").orEmpty()
                .ifBlank { obj?.s("title").orEmpty() }
                .ifBlank { obj?.s("text").orEmpty() }
                .ifBlank { raw }
        }.replace('\n', ' ')
    }

    // ---------------------------------------------------------------- parsing

    private fun parseFeedVideo(item: JSONObject): VideoItem? {
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return null
        val owner = item.optJSONObject("owner")
        val stat = item.optJSONObject("stat")
        return VideoItem(
            bvid = bvid,
            aid = item.l("id").takeIf { it > 0 } ?: item.l("aid"),
            title = cleanHtml(item.s("title")),
            cover = image(item.s("pic"), 480),
            ownerName = owner?.s("name").orEmpty(),
            ownerFace = avatar(owner?.s("face").orEmpty(), 96),
            ownerMid = owner?.l("mid") ?: 0,
            play = stat?.l("view") ?: 0,
            danmaku = stat?.l("danmaku") ?: 0,
            duration = item.optInt("duration", 0),
            pubdate = item.l("pubdate"),
            cid = item.l("cid"),
            categoryName = item.s("tname"),
            recommendationTrackId = item.s("track_id"),
            reason = item.optJSONObject("rcmd_reason")?.s("content").orEmpty(),
            isFollowed = item.optBoolean("is_followed", false),
        )
    }

    private fun parseSpaceVideo(item: JSONObject): VideoItem? {
        val bvid = item.s("bvid")
        if (bvid.isBlank()) return null
        return VideoItem(
            bvid = bvid,
            aid = item.l("aid"),
            title = cleanHtml(item.s("title")),
            cover = image(item.s("pic"), 480),
            ownerName = item.s("author"),
            ownerFace = "",
            ownerMid = item.l("mid"),
            play = item.l("play"),
            danmaku = item.l("video_review"),
            duration = parseDurationText(item.s("length")),
            pubdate = item.l("created"),
            cid = item.l("cid"),
        )
    }

    private fun parseVideoDetail(data: JSONObject): VideoDetail {
        val owner = data.optJSONObject("owner")
        val stat = data.optJSONObject("stat")
        val pages = data.optJSONArray("pages").objects().map { page ->
            VideoPage(
                cid = page.l("cid"),
                page = page.optInt("page", 1),
                part = page.s("part"),
                duration = page.optInt("duration", 0),
            )
        }
        val req = data.optJSONObject("req_user")
        val episodes = data.optJSONObject("ugc_season")
            ?.optJSONArray("sections").objects()
            ?.flatMap { section -> section.optJSONArray("episodes").objects() }
            ?.mapNotNull { ep ->
                val arc = ep.optJSONObject("arc")
                val bvid = ep.s("bvid").ifBlank { arc?.s("bvid").orEmpty() }
                val cid = ep.l("cid")
                if (bvid.isBlank() || cid <= 0) return@mapNotNull null
                VideoEpisode(
                    bvid = bvid,
                    aid = ep.l("aid").takeIf { it > 0 } ?: (arc?.l("aid") ?: 0L),
                    cid = cid,
                    title = ep.s("title").ifBlank { arc?.s("title").orEmpty() },
                    cover = image(arc?.s("pic").orEmpty(), 480),
                    duration = arc?.optInt("duration", 0) ?: 0,
                    play = (arc?.optJSONObject("stat") ?: ep.optJSONObject("stat"))?.l("view") ?: 0L,
                    danmaku = (arc?.optJSONObject("stat") ?: ep.optJSONObject("stat"))?.l("danmaku") ?: 0L,
                    pubdate = arc?.l("pubdate") ?: ep.l("pubdate"),
                )
            }
            .orEmpty()
        return VideoDetail(
            bvid = data.s("bvid"),
            aid = data.l("aid"),
            title = cleanHtml(data.s("title")),
            cover = image(data.s("pic"), 1200),
            desc = data.s("desc"),
            ownerName = owner?.s("name").orEmpty(),
            ownerFace = avatar(owner?.s("face").orEmpty(), 160),
            ownerMid = owner?.l("mid") ?: 0,
            ownerSign = "",
            play = stat?.l("view") ?: 0,
            danmaku = stat?.l("danmaku") ?: 0,
            like = stat?.l("like") ?: 0,
            coin = stat?.l("coin") ?: 0,
            favorite = stat?.l("favorite") ?: 0,
            share = stat?.l("share") ?: 0,
            reply = stat?.l("reply") ?: 0,
            pubdate = data.l("pubdate"),
            duration = data.optInt("duration", 0),
            cid = data.l("cid"),
            tname = data.s("tname"),
            pages = pages,
            episodes = episodes,
            seasonTitle = data.optJSONObject("ugc_season")?.s("title").orEmpty(),
            liked = req?.optInt("like", 0) == 1,
            coined = (req?.optInt("coin", 0) ?: 0) > 0,
            favorited = (req?.optInt("favorite", 0) ?: 0) > 0,
        )
    }

    /** 网络类型、VPN、系统代理：用来判断是不是代理/VPN 把 playurl 换成了空内容。 */
    private fun netInfo(): String {
        val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
            as? android.net.ConnectivityManager
        val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
        val transports = buildList {
            if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
            if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cell")
            if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
            if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
        }.joinToString("+").ifBlank { "none" }
        val proxy = runCatching {
            java.net.ProxySelector.getDefault()
                ?.select(java.net.URI("https://api.bilibili.com"))
                ?.firstOrNull()
                ?.toString()
        }.getOrNull()
        return "net=$transports vpn=${caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)} " +
            "proxy=$proxy"
    }

    /** 播放地址接口返回内容的形状，写进应用日志，方便定位“有 data 但没有地址”。 */
    private fun describePlayData(data: JSONObject): String {
        val dash = data.optJSONObject("dash")
        val video = dash?.optJSONArray("video")
        val audio = dash?.optJSONArray("audio")
        val durl = data.optJSONArray("durl")
        val firstRep = video?.optJSONObject(0)
        val firstRaw = firstRep?.let { it.s("baseUrl").ifBlank { it.s("base_url") } }.orEmpty()
        val first = firstRaw.substringBefore('?')
        val repKeys = firstRep?.keys()?.asSequence()?.take(12)?.joinToString(",").orEmpty()
        val dataKeys = data.keys().asSequence().take(16).joinToString(",")
        return "dataKeys=$dataKeys dash=${dash != null} video=${video?.length() ?: 0} " +
            "audio=${audio?.length() ?: 0} durl=${durl?.length() ?: 0} " +
            "firstBaseLen=${firstRaw.length} repKeys=$repKeys first=${first.take(80)}"
    }

    private fun parsePlayInfo(
        data: JSONObject,
        preferred: Int,
        detail: VideoDetail,
        durationSec: Int = detail.duration,
        codecPreference: String = "auto",
        cdnService: String = "base",
    ): PlayInfo {
        val cdn = cdnServiceById(cdnService)
        val dash = data.optJSONObject("dash")
        val audio = chooseAudio(dash?.optJSONArray("audio"), cdn)
        val qualities = mutableListOf<Quality>()
        dash?.optJSONArray("video")?.objects()?.groupBy { it.optInt("id", 0) }?.forEach { (id, reps) ->
            val wanted = if (codecPreference == "auto") {
                emptyList()
            } else {
                reps.filter { codecMatches(it.s("codecs"), codecPreference) }
            }
            val avc = reps.filter { it.s("codecs").contains("avc", true) }
            val pool = wanted.ifEmpty { avc.ifEmpty { reps } }
            // 自动模式下不要盲目选最高码率：4K 常见 AV1/HEVC 两版，AV1 很多设备硬解不了，
            // 选错会触发 480P 兜底，表现出来就是“选了 4K 但很糊”。
            // 兼容性优先：AVC > HEVC > AV1 > 其他；同编码内再取最高码率。
            val rep = if (wanted.isNotEmpty()) {
                wanted.maxByOrNull { it.l("bandwidth") }
            } else {
                pool.sortedWith(
                    compareBy<JSONObject> { codecPriority(it.s("codecs")) }
                        .thenByDescending { it.l("bandwidth") },
                ).firstOrNull()
            } ?: return@forEach
            val baseUrl = rep.s("baseUrl").ifBlank { rep.s("base_url") }
            if (baseUrl.isBlank()) return@forEach
            val backups = jsonStrings(
                rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url"),
            )
            qualities += Quality(
                id = id,
                label = qualityLabel(id),
                width = rep.optInt("width", 0),
                height = rep.optInt("height", 0),
                frameRate = rep.s("frameRate").ifBlank { rep.s("fps") },
                codec = rep.s("codecs"),
                videoUrl = cdn.rewrite(baseUrl, backups),
                rawVideoUrl = baseUrl,
            )
        }
        val fallback = data.optJSONArray("durl")?.optJSONObject(0)?.s("url").orEmpty()
        val accept = data.optJSONArray("accept_quality")?.let { array ->
            (0 until array.length()).map { array.optInt(it, 0) }
        }.orEmpty()
        return PlayInfo(
            qualities = qualities.sortedByDescending { it.id },
            audioUrl = audio?.first,
            rawAudioUrl = audio?.second,
            fallbackUrl = fallback.takeIf { it.isNotBlank() }?.let { cdn.rewrite(it) },
            rawFallbackUrl = fallback.takeIf { it.isNotBlank() },
            durationMs = data.l("timelength").takeIf { it > 0 } ?: (durationSec * 1000L),
            acceptQuality = accept,
        )
    }

    private fun chooseAudio(array: JSONArray?, cdn: CdnService): Pair<String, String>? {
        val audios = array.objects()
        if (audios.isEmpty()) return null
        val standard = audios.filter { it.optInt("id", 0) in setOf(30216, 30232, 30280) }
        val selected = standard.maxByOrNull { it.optInt("id", 0) } ?: audios.minByOrNull { it.l("bandwidth") }
        return selected?.let { audio ->
            val url = audio.s("baseUrl").ifBlank { audio.s("base_url") }
            cdn.rewrite(
                url,
                jsonStrings(audio.optJSONArray("backupUrl") ?: audio.optJSONArray("backup_url")),
            ) to url
        }
    }

    /** 自动选编码时的兼容性优先级，数字越小越优先。 */
    private fun codecPriority(codec: String): Int {
        val value = codec.lowercase()
        return when {
            value.contains("avc") || value.contains("h264") -> 0
            value.contains("hev") || value.contains("hvc") || value.contains("h265") -> 1
            value.contains("av01") || value.contains("av1") -> 2
            else -> 3
        }
    }

    private fun codecMatches(codec: String, preference: String): Boolean {
        val value = codec.lowercase()
        return when (preference.lowercase()) {
            "avc" -> value.contains("avc")
            "hevc" -> value.contains("hev") || value.contains("hvc")
            "av1" -> value.contains("av01") || value.contains("av1")
            "dvh1" -> value.contains("dvh1") || value.contains("dvhe") || value.contains("dolby")
            else -> true
        }
    }

    private fun jsonStrings(array: JSONArray?): List<String> {
        if (array == null) return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            array.optString(index).takeIf { it.isNotBlank() }
        }
    }

    /** 给 CDN 设置测速用一个固定公开视频流。 */
    suspend fun cdnSampleUrl(): String? = withContext(Dispatchers.IO) {
        val data = runCatching {
            dataRequest(
                "/x/player/playurl",
                mapOf(
                    "bvid" to "BV1fK4y1t7hj",
                    "cid" to "196018899",
                    "qn" to "80",
                    "fnval" to "4048",
                    "fnver" to "0",
                    "fourk" to "1",
                ),
            )
        }.getOrNull() ?: return@withContext null
        val rep = data.optJSONObject("dash")
            ?.optJSONArray("video")
            ?.objects()
            ?.firstOrNull()
            ?: return@withContext null
        val urls = buildList {
            add(rep.s("baseUrl").ifBlank { rep.s("base_url") })
            addAll(jsonStrings(rep.optJSONArray("backupUrl") ?: rep.optJSONArray("backup_url")))
        }.filter { it.isNotBlank() }
        // 优先用 /upgcxcode/ 形态的地址：mcdn 的 /vN/resource 地址换 host 会 403，不能用来测速。
        urls.firstOrNull { it.contains("/upgcxcode/") && !isMcdnResource(it) }
            ?: urls.firstOrNull { it.contains("/upgcxcode/") }
            ?: urls.firstOrNull()
    }

    private fun decodeDanmakuBytes(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""
        val decompressed = runCatching {
            when {
                bytes.size > 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte() -> {
                    GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                }
                bytes.size > 2 && bytes[0] == 0x78.toByte() -> {
                    InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                }
                bytes[0] == '<'.code.toByte() -> bytes
                else -> {
                    java.util.zip.InflaterInputStream(
                        ByteArrayInputStream(bytes),
                        java.util.zip.Inflater(true),
                    ).use { it.readBytes() }
                }
            }
        }.getOrNull()
        return String(decompressed ?: bytes, Charsets.UTF_8)
    }

    private fun parseDanmakuXml(xml: String): List<DanmakuItem> {
        if (xml.isBlank()) return emptyList()
        val result = ArrayList<DanmakuItem>(512)
        runCatching {
            val parser = org.xmlpull.v1.XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(java.io.StringReader(xml))
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && result.size < 4000) {
                if (event == XmlPullParser.START_TAG && parser.name == "d") {
                    val p = parser.getAttributeValue(null, "p").orEmpty()
                    val text = parser.nextText().orEmpty().trim()
                    val parts = p.split(',')
                    if (parts.size >= 4 && text.isNotBlank() && text.length <= 90) {
                        val seconds = parts[0].toFloatOrNull() ?: 0f
                        val mode = parts[1].toIntOrNull() ?: 1
                        if (mode == 1 || mode == 4 || mode == 5 || mode == 7 || mode == 8) {
                            result += DanmakuItem(
                                timeMs = (seconds * 1000).toLong(),
                                mode = mode,
                                color = parts[3].toIntOrNull() ?: 0xFFFFFF,
                                size = parts.getOrNull(2)?.toFloatOrNull() ?: 25f,
                                text = text,
                            )
                        }
                    }
                }
                event = parser.next()
            }
        }
        return result.sortedBy { it.timeMs }
    }

    private fun parseComment(
        reply: JSONObject,
        upMid: Long,
        top: Boolean,
        depth: Int = 0,
    ): CommentItem {
        val member = reply.optJSONObject("member")
        val content = reply.optJSONObject("content")
        val mid = member?.l("mid") ?: 0
        val emotes = mutableMapOf<String, String>()
        content?.optJSONObject("emote")?.let { obj ->
            obj.keys().forEach { rawKey ->
                // 接口里的 key 是 "[doge]"，正文里是 [doge]，统一成不带括号的 "doge"
                val key = rawKey.removeSurrounding("[", "]").trim()
                val url = obj.optJSONObject(rawKey)?.s("url").orEmpty()
                if (key.isNotBlank() && url.isNotBlank()) emotes[key] = url
            }
        }
        val pictures = content?.optJSONArray("pictures")?.objects()?.mapNotNull { pic ->
            val url = pic.s("img_src").ifBlank { pic.s("url") }
            if (url.isBlank()) null else image(url, 720)
        }.orEmpty()
        // 正文里 @ 的人；官方接口会在 content.members 里给出 mid。
        val mentions = content?.optJSONArray("members")?.objects()?.mapNotNull { m ->
            val name = m.s("uname").ifBlank { m.s("name") }
            val memberMid = m.l("mid")
            if (name.isBlank() || memberMid <= 0L) null else CommentMention(memberMid, name)
        }.orEmpty()
        return CommentItem(
            rpid = reply.l("rpid"),
            uname = member?.s("uname").orEmpty(),
            avatar = avatar(member?.s("avatar").orEmpty(), 120),
            message = content?.s("message").orEmpty(),
            like = reply.optInt("like", 0),
            ctime = reply.l("ctime"),
            level = member?.optJSONObject("level_info")?.optInt("current_level") ?: 0,
            replyCount = reply.optInt("rcount", 0),
            isUp = upMid > 0 && mid == upMid,
            isTop = top,
            liked = reply.optInt("action", 0) == 1,
            mid = mid,
            oid = reply.l("oid"),
            root = reply.l("root"),
            parent = reply.l("parent"),
            emotes = emotes,
            ipLocation = reply.optJSONObject("reply_control")?.s("location").orEmpty(),
            isVip = member?.optJSONObject("vip")?.optInt("vipStatus", 0) == 1,
            pictures = pictures,
            mentions = mentions,
            previewReplies = if (depth < 1) {
                reply.optJSONArray("replies")?.objects()
                    ?.take(3)
                    ?.map { parseComment(it, upMid, false, depth + 1) }
                    .orEmpty()
            } else {
                emptyList()
            },
        )
    }

    // ---------------------------------------------------------------- live

    private val liveBase = "https://api.live.bilibili.com"

    /**
     * 直播间信息（标题 / 主播 / 封面 / 人气 / 分区）。
     * 接口不通时返回 null，界面会退回 mock 数据，不会白屏。
     */
    suspend fun liveRoomInfo(roomId: Long): LiveRoomInfo? = withContext(Dispatchers.IO) {
        val referer = "https://live.bilibili.com/$roomId"
        val json = runCatching {
            rawRequest(
                "/xlive/web-room/v1/index/getInfoByRoom",
                mapOf("room_id" to roomId.toString()),
                base = liveBase,
                referer = referer,
            )
        }.getOrNull() ?: return@withContext null
        if (json.optInt("code", -1) != 0) return@withContext null
        val data = json.optJSONObject("data") ?: return@withContext null
        val info = data.optJSONObject("room_info") ?: return@withContext null
        val anchor = data.optJSONObject("anchor_info")?.optJSONObject("base_info")
        LiveRoomInfo(
            roomId = info.optLong("room_id", roomId),
            title = info.optString("title"),
            uname = anchor?.optString("uname").orEmpty(),
            uid = info.optLong("uid"),
            face = normalizeLiveUrl(anchor?.optString("face").orEmpty()),
            cover = normalizeLiveUrl(
                info.optString("cover").ifBlank { info.optString("keyframe") },
            ),
            onlineText = liveCountText(info.optLong("online")),
            areaText = info.optString("area_name"),
            following = false,
            live = info.optInt("live_status", 1) == 1,
        )
    }

    /**
     * 随便挑一个正在直播的房间号（首页推荐列表第一条）。
     * 用于「没有指定房间也能看看效果」；全站都没开播时返回 null。
     */
    suspend fun liveHotRoomId(): Long? = withContext(Dispatchers.IO) {
        for (area in listOf("1", "9", "0")) {
            val json = runCatching {
                rawRequest(
                    "/xlive/web-interface/v1/second/getList",
                    mapOf(
                        "platform" to "web",
                        "parent_area_id" to area,
                        "area_id" to "0",
                        "sort_type" to "online",
                        "page" to "1",
                        "page_size" to "1",
                    ),
                    base = liveBase,
                    referer = "https://live.bilibili.com/",
                )
            }.getOrNull() ?: continue
            val id = json.optJSONObject("data")
                ?.optJSONArray("list")
                ?.optJSONObject(0)
                ?.optLong("roomid", 0L) ?: 0L
            if (id > 0L) return@withContext id
        }
        null
    }

    /**
     * 直播流地址。先按 web 端参数要一遍；失败时再带 WBI 签名重试一次。
     */
    suspend fun livePlayUrl(roomId: Long, qn: Int = 10000): String? = withContext(Dispatchers.IO) {
        val referer = "https://live.bilibili.com/$roomId"
        val params = mapOf(
            "room_id" to roomId.toString(),
            "protocol" to "0,1",
            "format" to "0,1,2",
            "codec" to "0,1",
            "qn" to qn.toString(),
            "platform" to "web",
            "ptype" to "8",
            "dolby" to "5",
            "panorama" to "1",
            "web_location" to "444.8",
        )
        fun pick(json: JSONObject?): String? = json?.let(::parseLivePlayUrl)
        val plain = runCatching {
            rawRequest(
                "/xlive/web-room/v2/index/getRoomPlayInfo",
                params,
                base = liveBase,
                referer = referer,
            )
        }.getOrNull()
        pick(plain)?.let { return@withContext it }
        val signed = runCatching {
            rawRequest(
                "/xlive/web-room/v2/index/getRoomPlayInfo",
                params,
                signed = true,
                base = liveBase,
                referer = referer,
            )
        }.getOrNull()
        pick(signed)
    }

    private fun parseLivePlayUrl(json: JSONObject): String? {
        val streams = json.optJSONObject("data")
            ?.optJSONObject("playurl_info")
            ?.optJSONObject("playurl")
            ?.optJSONArray("stream") ?: return null
        var fallback: String? = null
        for (i in 0 until streams.length()) {
            val formats = streams.optJSONObject(i)?.optJSONArray("format") ?: continue
            for (j in 0 until formats.length()) {
                val codecs = formats.optJSONObject(j)?.optJSONArray("codec") ?: continue
                for (k in 0 until codecs.length()) {
                    val codec = codecs.optJSONObject(k) ?: continue
                    val infos = codec.optJSONArray("url_info") ?: continue
                    val first = infos.optJSONObject(0) ?: continue
                    val url = first.optString("host") +
                        codec.optString("base_url") +
                        first.optString("extra")
                    if (url.isBlank()) continue
                    if (fallback == null) fallback = url
                    // 直播优先 FLV + AVC：兼容性最好，Media3 直接能播
                    if (codec.optString("codec_name") == "avc" &&
                        codec.optString("format").ifBlank { formats.optJSONObject(j)?.optString("format") } == "flv"
                    ) {
                        return url
                    }
                }
            }
        }
        return fallback
    }

    /** 直播接口有时返回 //i0.hdslb.com/... 这种省略协议头的地址。 */
    private fun normalizeLiveUrl(url: String): String = when {
        url.isBlank() -> ""
        url.startsWith("//") -> "https:$url"
        else -> url
    }

    /** 人气数：1.2 万 / 356 这种中文习惯写法。 */
    private fun liveCountText(count: Long): String = when {
        count <= 0 -> ""
        count >= 100_000_000 -> String.format(java.util.Locale.US, "%.1f 亿", count / 100_000_000.0)
        count >= 10_000 -> String.format(java.util.Locale.US, "%.1f 万", count / 10_000.0)
        else -> count.toString()
    }

    // ---------------------------------------------------------------- request

    private class WbiKeys(val imgKey: String, val subKey: String) {
        val mixinKey: String = run {
            val raw = imgKey + subKey
            MIXIN_TABLE.map { raw.getOrElse(it) { '0' } }.joinToString("").take(32)
        }
    }

    private suspend fun ensureBuvid() {
        if (cookieJar.value("buvid3").isNotBlank()) return
        if (System.currentTimeMillis() < buvidRetryAt) return
        buvidMutex.withLock {
            if (cookieJar.value("buvid3").isNotBlank()) return
            if (System.currentTimeMillis() < buvidRetryAt) return
            val ok = try {
                val request = Request.Builder()
                    .url("$API/x/frontend/finger/spi")
                    .header("User-Agent", UA)
                    .header("Referer", "https://www.bilibili.com/")
                    .get()
                    .build()
                client.newCall(request).awaitResponse().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                    val body = response.body?.string().orEmpty()
                    val data = JSONObject(body).optJSONObject("data")
                        ?: throw IOException("buvid data is empty")
                    val b3 = data.s("b_3")
                    if (b3.isBlank()) throw IOException("buvid3 is empty")
                    val b4 = data.s("b_4")
                    cookieJar.put("buvid3", b3)
                    if (b4.isNotBlank()) cookieJar.put("buvid4", b4)
                    cookieJar.put("b_nut", (System.currentTimeMillis() / 1000).toString())
                    true
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) android.util.Log.w("BiliApi", "buvid fetch failed", e)
                false
            }
            if (ok) {
                buvidRetryAt = 0L
            } else {
                // 失败后 30 秒内不再重试，否则每个接口都会先卡一次 buvid 请求。
                buvidRetryAt = System.currentTimeMillis() + 30_000L
            }
        }
    }

    private suspend fun ensureWbiKeys(): WbiKeys {
        val cached = wbiKeys
        if (cached != null &&
            System.currentTimeMillis() - wbiFetchedAt < WBI_TTL_MS
        ) {
            return cached
        }
        wbiMutex.withLock {
            val current = wbiKeys
            if (current != null &&
                System.currentTimeMillis() - wbiFetchedAt < WBI_TTL_MS
            ) {
                return current
            }
            val fallback = current
            try {
                val root = rawRequest("/x/web-interface/nav", emptyMap())
                parseWbi(root.optJSONObject("data")?.optJSONObject("wbi_img"))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // 刷新失败但旧 key 还在时继续用旧 key，别让整个推荐/搜索直接不可用。
                if (fallback != null) return fallback
                throw BiliException(-1, "无法获取请求签名，请检查网络")
            }
            return wbiKeys ?: fallback ?: throw BiliException(-1, "无法获取请求签名，请检查网络")
        }
    }

    private fun parseWbi(wbi: JSONObject?) {
        if (wbi == null) return
        val img = wbi.s("img_url").substringAfterLast('/').substringBefore('.')
        val sub = wbi.s("sub_url").substringAfterLast('/').substringBefore('.')
        if (img.isNotBlank() && sub.isNotBlank()) {
            wbiKeys = WbiKeys(img, sub)
            wbiFetchedAt = System.currentTimeMillis()
        }
    }

    private suspend fun dataRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = API,
        method: String = "GET",
        referer: String? = null,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val root = rawRequest(
            path,
            params,
            signed,
            base,
            method,
            referer,
            userAgent = userAgent,
            headers = headers,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        return root.optJSONObject("data") ?: JSONObject()
    }

    private class RetryableHttpException(val statusCode: Int) : IOException("HTTP $statusCode")

    /** OkHttp 的异步回调转成可取消的 suspend，避免 execute() 占死 IO 线程且无法随页面取消。 */
    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            },
        )
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }

    private fun retryDelayMs(attempt: Int): Long =
        350L * (1L shl attempt) + (0..180).random().toLong()

    private suspend fun executeOnce(
        request: Request,
        tolerateEmptyBody: Boolean,
    ): JSONObject = client.newCall(request).awaitResponse().use { response ->
        if (response.code == 408 || response.code == 429 || response.code >= 500) {
            throw RetryableHttpException(response.code)
        }
        if (response.code >= 400) {
            throw BiliException(response.code, "网络错误 HTTP ${response.code}")
        }
        val body = response.body?.string().orEmpty()
        if (request.url.encodedPath.contains("playurl")) {
            val safeBody = body
                .replace(Regex("https?://[^\"\\s]+"), "[url]")
                .replace(
                    Regex("\"(e|upsig|sign|deadline|trid|w_rid|uipk|qn_dyeid)\":\"[^\"]*\""),
                    "\"\$1\":\"[redacted]\"",
                )
                .take(1600)
            LogCollector.record(
                "API",
                "http playurl code=${response.code} type=${response.header("Content-Type")} " +
                    "len=${response.header("Content-Length")} bodyLen=${body.length} " +
                    "encoding=${response.header("Content-Encoding")} " +
                    "server=${response.header("Server")} via=${response.header("Via")} " +
                    "body=$safeBody",
            )
        }
        if (body.isBlank()) {
            // 点赞/投币/收藏等动作接口成功时偶尔返回 2xx 空 body，不能当成“解析失败”
            if (tolerateEmptyBody && response.code in 200..299) return@use JSONObject()
            throw BiliException(-1, "服务器返回为空")
        }
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw BiliException(-1, "数据解析失败")
        }
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "BiliApi",
                "response ${request.url} http=${response.code} code=${json.optInt("code", 0)}",
            )
        }
        json
    }

    private suspend fun rawRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = API,
        method: String = "GET",
        referer: String? = null,
        tolerateEmptyBody: Boolean = false,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject = withContext(Dispatchers.IO) {
        ensureBuvid()
        val actualParams = params.toMutableMap()
        var url = "$base$path"
        if (signed) {
            val keys = ensureWbiKeys()
            actualParams["wts"] = (System.currentTimeMillis() / 1000).toString()
            val query = actualParams.toSortedMap().entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value.replace(Regex("[!'()*]"), ""))}"
            }
            val rid = md5(query + keys.mixinKey)
            url = "$url?$query&w_rid=$rid"
        } else if (actualParams.isNotEmpty()) {
            val query = actualParams.entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }
            url = "$url?$query"
        }
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent ?: UA)
            .header("Referer", referer ?: if (base == PASSPORT) "https://passport.bilibili.com/" else "https://www.bilibili.com/")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
        headers.forEach { (key, value) -> builder.header(key, value) }
        if (method == "POST") {
            val form = FormBody.Builder()
            actualParams.forEach { (key, value) -> form.add(key, value) }
            builder.post(form.build())
                .header("Origin", "https://www.bilibili.com")
        }
        val request = builder.build()
        if (BuildConfig.DEBUG) android.util.Log.d("BiliApi", "request ${request.url.encodedPath}")

        // 只对 GET 做自动重试：POST 可能是投币/点赞，重复发送会有副作用。
        // 最多两次：一次正常、一次重试；避免网络完全断开时转圈太久。
        val attempts = if (method == "GET") 2 else 1
        var lastError: Exception? = null
        for (attempt in 0 until attempts) {
            try {
                return@withContext executeOnce(request, tolerateEmptyBody)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BiliException) {
                // 200 空 body / 解析失败也当作可重试的瞬时问题（仅 GET）。
                if (method == "GET" && e.code == -1 && attempt < attempts - 1) {
                    lastError = e
                } else {
                    throw e
                }
            } catch (e: RetryableHttpException) {
                lastError = BiliException(e.statusCode, "网络错误 HTTP ${e.statusCode}")
            } catch (e: IOException) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            } catch (e: Exception) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            }
            if (attempt < attempts - 1) delay(retryDelayMs(attempt))
        }
        val error = lastError ?: BiliException(-1, "网络连接失败，请检查网络后重试")
        LogCollector.record(
            "API",
            "fail path=${request.url.encodedPath} status=${(error as? BiliException)?.code} " +
                "message=${error.message}",
        )
        if (BuildConfig.DEBUG) android.util.Log.w("BiliApi", "network error ${request.url.encodedPath}", error)
        throw error
    }

    private suspend fun postRequest(
        path: String,
        params: Map<String, String>,
        tolerateEmptyBody: Boolean = true,
    ): JSONObject {
        val result = rawRequest(
            path,
            params,
            method = "POST",
            tolerateEmptyBody = tolerateEmptyBody,
        )
        val code = result.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, result.optString("message")))
        return result.optJSONObject("data") ?: JSONObject()
    }

    /**
     * 给 app.bilibili.com 的接口补上 App 签名。
     * 规则：params 按 key 排序 → 拼成 a=b&c=d → 末尾接 appsec → MD5。
     * 网页的 WBI 签名在这类接口上无效，会直接返回 -400。
     */
    private fun signAppParams(
        params: MutableMap<String, String>,
        appKey: String = APP_KEY,
        appSec: String = APP_SEC,
        includeAccessKey: Boolean = true,
    ) {
        params["appkey"] = appKey
        params["ts"] = (System.currentTimeMillis() / 1000L).toString()
        if (includeAccessKey) {
            cookieJar.value("access_key").takeIf { it.isNotBlank() }?.let {
                params["access_key"] = it
            }
        }
        val query = params.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        params["sign"] = md5(query + appSec)
    }

    companion object {
        const val API = "https://api.bilibili.com"
    const val APP_API = "https://app.bilibili.com"
    const val APP_UA =
        "Mozilla/5.0 BiliDroid/8.43.0 (bbcallen@gmail.com) os/android model/android " +
            "mobi_app/android build/8430300 channel/master innerVer/8430300 osVer/15 network/2"
        const val APP = "https://app.bilibili.com"
        /** Android App 的 appkey / appsec，用于 app.bilibili.com 的签名。 */
        private const val APP_KEY = "dfca71928277209b"
        private const val APP_SEC = "b5475a8825547a4fc26c7d518eaaa02e"
        /** TV 端 appkey，用于 /x/passport-tv-login 扫码换 access_key。 */
        private const val TV_APP_KEY = "4409e2ce8ffd12b8"
        private const val TV_APP_SEC = "59b43e04ad6965f34319062b478f83dd"
        private val APP_HEADERS = mapOf(
            "env" to "prod",
            "app-key" to "android64",
            "x-bili-aurora-zone" to "sh001",
        )
        const val PASSPORT = "https://passport.bilibili.com"
        const val VC = "https://api.vc.bilibili.com"
        const val UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"

        /** WBI key 大约每天轮换；6 小时刷新一次足够，也避免进程活久了签名一直用旧 key。 */
        private const val WBI_TTL_MS = 6 * 60 * 60 * 1000L

        private val MIXIN_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )

        private fun encode(value: String): String = Uri.encode(value, "-_.~")

        private fun md5(value: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            return digest.joinToString("") { "%02x".format(it.toInt() and 0xff) }
        }

        fun image(url: String, width: Int = 0): String {
            if (url.isBlank()) return ""
            var result = when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
                else -> url
            }
            if (width > 0 && !result.contains("@")) {
                result += "@${width}w_${(width * 9 / 16)}h_1c.webp"
            }
            return result
        }

        fun avatar(url: String, size: Int): String {
            if (url.isBlank()) return ""
            var result = when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("http://") -> "https://${url.removePrefix("http://")}"
                else -> url
            }
            if (!result.contains("@")) {
                result += "@${size}w_${size}h_1c.webp"
            }
            return result
        }

        fun cleanHtml(text: String): String {
            if (text.isBlank()) return ""
            return runCatching {
                Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
            }.getOrDefault(text.replace(Regex("<[^>]+>"), ""))
        }

        /** App 推荐接口只给 aid，不给 bvid；用 AV/BV 官方换算规则在本地转。 */
        fun aidToBvid(aid: Long): String {
            if (aid <= 0L) return ""
            val table = "fZodR9XQDSUm21yCkr6zBqiveYah8bt4xsWpHnJE7jL5VG3guMTKNPAwcF"
            val positions = intArrayOf(11, 10, 3, 8, 4, 6)
            val result = charArrayOf('B', 'V', '1', ' ', ' ', '4', ' ', '1', ' ', '7', ' ', ' ')
            val x = (aid xor 177451812L) + 8728348608L
            var power = 1L
            for (i in 0..5) {
                result[positions[i]] = table[((x / power) % 58L).toInt()]
                power *= 58L
            }
            return String(result)
        }

        /** 动态接口的播放量/弹幕数有时是「14.6万」「1.2亿」这种文字，不能直接 toLong。 */
        fun parseCountText(text: String): Long {
            val raw = text.trim()
            if (raw.isBlank() || raw == "-" || raw == "--") return 0L
            val multiplier = when {
                raw.endsWith("亿") -> 100_000_000.0
                raw.endsWith("万") -> 10_000.0
                else -> 1.0
            }
            val number = raw.removeSuffix("亿").removeSuffix("万").toDoubleOrNull() ?: return 0L
            return (number * multiplier).toLong()
        }

        fun parseDurationText(text: String): Int {
            if (text.isBlank() || text == "0") return 0
            return runCatching {
                val parts = text.split(":")
                when (parts.size) {
                    3 -> parts[0].toInt() * 3600 + parts[1].toInt() * 60 + parts[2].toInt()
                    2 -> parts[0].toInt() * 60 + parts[1].toInt()
                    else -> text.toIntOrNull() ?: 0
                }
            }.getOrDefault(0)
        }

        fun qualityLabel(id: Int): String = when (id) {
            127 -> "8K"
            126 -> "杜比视界"
            125 -> "HDR"
            120 -> "4K"
            116 -> "1080P60"
            112 -> "1080P+"
            80 -> "1080P"
            74 -> "720P60"
            64 -> "720P"
            32 -> "480P"
            16 -> "360P"
            else -> "${id}P"
        }
    }
}

internal fun JSONObject.s(key: String): String {
    if (isNull(key)) return ""
    return optString(key, "").takeIf { it != "null" } ?: ""
}

internal fun JSONObject.l(key: String): Long = optLong(key, 0L)

internal fun JSONArray?.objects(): List<JSONObject> {
    if (this == null) return emptyList()
    val result = ArrayList<JSONObject>(length())
    for (i in 0 until length()) {
        optJSONObject(i)?.let(result::add)
    }
    return result
}

/** 私信会话名字/头像的小缓存，避免每页重复请求同一批用户。 */
private object MessageUserCache {
    private const val MAX = 200
    private val map = object : LinkedHashMap<Long, Pair<String, String>>(MAX, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<Long, Pair<String, String>>?,
        ): Boolean = size > MAX
    }

    @Synchronized
    fun get(mid: Long): Pair<String, String>? = map[mid]

    @Synchronized
    fun put(mid: Long, name: String, avatar: String) {
        if (mid > 0L && name.isNotBlank()) map[mid] = name to avatar
    }
}

private object SponsorSegmentCache {
    private val values = object : LinkedHashMap<String, List<SponsorSegment>>(100, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, List<SponsorSegment>>?,
        ): Boolean = size > 100
    }

    @Synchronized
    fun get(bvid: String): List<SponsorSegment>? = values[bvid]

    @Synchronized
    fun put(bvid: String, list: List<SponsorSegment>) {
        values[bvid] = list
    }
}
