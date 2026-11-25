package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.danmakuUserHash

import android.content.Context
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
import kotlin.math.roundToInt
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.infra.bili.mapper.*
import com.aritxonly.bdfun.infra.bili.BILI_API as API
import com.aritxonly.bdfun.infra.bili.BILI_APP_API as APP_API
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA as APP_UA
import com.aritxonly.bdfun.infra.bili.BILI_APP as APP
import com.aritxonly.bdfun.infra.bili.BILI_APP_KEY as APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_APP_SEC as APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_KEY as TV_APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_SEC as TV_APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_APP_HEADERS as APP_HEADERS
import com.aritxonly.bdfun.infra.bili.BILI_PASSPORT as PASSPORT
import com.aritxonly.bdfun.infra.bili.BILI_VC as VC
import com.aritxonly.bdfun.infra.bili.BILI_UA as UA
import com.aritxonly.bdfun.infra.bili.encodeBiliParameter as encode
import com.aritxonly.bdfun.infra.bili.biliMd5 as md5
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml as cleanHtml
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.biliQualityLabel as qualityLabel

internal suspend fun BiliApiContext.view(bvid: String): VideoDetail = withContext(Dispatchers.IO) {
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

internal suspend fun BiliApiContext.viewByAid(aid: Long): VideoDetail = withContext(Dispatchers.IO) {
    val data = dataRequest("/x/web-interface/view", mapOf("aid" to aid.toString()))
    parseVideoDetail(data)
}

internal suspend fun BiliApiContext.playInfo(
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
    var root = cancellableCatching {
        rawRequest("/x/player/wbi/playurl", params, signed = true)
    }.onFailure { signedError = it }.getOrNull()
    val usedSigned = root != null
    if (root == null) {
        root = rawRequest("/x/player/playurl", params)
    }
    val code = root.optInt("code", 0)
    val dataValue = root.opt("data")
    val data = root.optJSONObject("data") ?: JSONObject()
    log(
        "API",
        "playurl signed=$usedSigned code=$code message=${root.optString("message")} " +
            "dataType=${dataValue?.javaClass?.simpleName ?: "null"} " +
            "loggedIn=$isLoggedIn mid=$mid sessLen=${cookieJar.value("SESSDATA").length} " +
            "buvidLen=${cookieJar.value("buvid3").length} wbi=${wbiSigner.hasKeys} " +
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
    log(
        "API",
        "playurl parsed qualities=${parsed.qualities.size} " +
            "audio=${parsed.audioUrl != null} fallback=${parsed.fallbackUrl != null} " +
            "accept=${parsed.acceptQuality}",
    )
    if (parsed.fallbackUrl == null && parsed.qualities.isNotEmpty()) {
        // DASH 解码失败时的兜底。只取 1080P：原来会串行再试 720P/480P，
        // 但绝大多数视频 1080P 就能拿到，失败时再靠播放器降档，不必每次都多发两个请求。
        val fallbackUrl = cancellableCatching {
            dataRequest(
                "/x/player/playurl",
                mapOf(
                    "bvid" to detail.bvid,
                    "cid" to cid.toString(),
                    "qn" to "80",
                    "fnval" to "1",
                    "fnver" to "0",
                ),
            )
        }.getOrNull()?.let(::readProgressiveFallback).orEmpty()
        if (fallbackUrl.isNotBlank()) parsed.copy(fallbackUrl = fallbackUrl) else parsed
    } else {
        parsed
    }
}

internal suspend fun BiliApiContext.related(bvid: String): List<VideoItem> = withContext(Dispatchers.IO) {
    // 该接口的 data 是数组而不是对象，不能用 dataRequest
    val root = rawRequest("/x/web-interface/archive/related", mapOf("bvid" to bvid))
    if (root.optInt("code", 0) != 0) return@withContext emptyList()
    mapRelatedVideos(root, this@related::parseFeedVideo)
}

/**
 * 拉弹幕：与 BiliPai 一致走 protobuf 分段接口 seg.so，按播放位置只取「当前段 ±1」。
 *
 * 首次进视频只加载第 1、2 段；后续分段由 [BiliApiContext.danmakuSegment] 随播放位置补齐。
 * 分段弹幕全部为空（UP 关闭 / 服务端没有弹幕）时才退回旧 XML 接口，并只保留窗口内的数据。
 */

internal suspend fun BiliApiContext.danmaku(cid: Long, durationSec: Int = 0): List<DanmakuItem> =
    danmakuWindow(cid, danmakuSegmentWindow(0L, danmakuSegmentCount(durationSec)))

/** 并发拉取指定分段（带进程内缓存），返回合并后的弹幕。 */
internal suspend fun BiliApiContext.danmakuWindow(cid: Long, indices: List<Int>): List<DanmakuItem> {
    if (cid <= 0L || indices.isEmpty()) return emptyList()
    val parsed = kotlinx.coroutines.coroutineScope {
        indices.distinct().map { index ->
            async { danmakuSegmentOrNull(cid, index)?.takeIf { it.isNotEmpty() } }
        }.awaitAll()
    }.filterNotNull()
    if (parsed.isNotEmpty()) return parsed.flatten()
    return danmakuXmlForWindow(cid, indices)
}

/** 单段弹幕；空分段与失败都返回 null，交由调用方决定是否回退。 */
internal suspend fun BiliApiContext.danmakuSegmentOrNull(cid: Long, index: Int): List<DanmakuItem>? {
    if (cid <= 0L || index <= 0) return null
    danmakuSegmentCache.get(cid, index)?.let { return it }
    val bytes = cancellableCatching { fetchDanmakuSegment(cid, index) }.getOrNull()
        ?: return null
    val items = cancellableCatching {
        mapDanmakuSegments(listOf(bytes), danmakuUserHash(mid))
    }.getOrNull() ?: return null
    danmakuSegmentCache.put(cid, index, items)
    return items
}

/** 公共入口：单段请求只返回该段（不再整支视频重下）。 */
internal suspend fun BiliApiContext.danmakuSegment(cid: Long, index: Int): List<DanmakuItem> =
    danmakuSegmentOrNull(cid, index).orEmpty()

/** XML 兜底：只保留窗口内的时间段，避免为了 6 分钟弹幕拉整支视频。 */
private suspend fun BiliApiContext.danmakuXmlForWindow(cid: Long, indices: List<Int>): List<DanmakuItem> {
    val all = danmakuXml(cid)
    if (all.isEmpty()) return emptyList()
    val ranges = indices.distinct().map { index ->
        val start = (index - 1) * DANMAKU_SEGMENT_MS
        start..(start + DANMAKU_SEGMENT_MS)
    }
    return all.filter { item -> ranges.any { item.timeMs in it } }
}

internal suspend fun BiliApiContext.fetchDanmakuSegment(cid: Long, index: Int): ByteArray? {
    val request = Request.Builder()
        .url(
            "https://api.bilibili.com/x/v2/dm/web/seg.so" +
                "?type=1&oid=$cid&segment_index=$index",
        )
        .header("User-Agent", UA)
        .header("Referer", "https://www.bilibili.com/")
        .get()
        .build()
    return http.await(request).use { response ->
        if (!response.isSuccessful) return null
        response.body?.bytes()?.takeIf { it.isNotEmpty() }
    }
}

internal suspend fun BiliApiContext.danmakuXml(cid: Long): List<DanmakuItem> = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url("$API/x/v1/dm/list.so?oid=$cid")
        .header("User-Agent", UA)
        .header("Referer", "https://www.bilibili.com/")
        .get()
        .build()
    val bytes = http.await(request).use { response ->
        if (!response.isSuccessful) return@withContext emptyList()
        response.body?.bytes() ?: return@withContext emptyList()
    }
    val xml = decodeDanmakuBytes(bytes)
    parseDanmakuXml(xml, danmakuUserHash(mid))
}

/**
 * 空降助手：从 BilibiliSponsorBlock 公共 API 拉取当前视频的片段。
 * 只在视频页进入时请求一次，结果缓存在内存里，不参与每帧逻辑。
 */

internal suspend fun BiliApiContext.sponsorSegments(bvid: String): List<SponsorSegment> = withContext(Dispatchers.IO) {
    if (bvid.isBlank()) return@withContext emptyList()
    sponsorSegments.get(bvid)?.let { return@withContext it }
    val result = cancellableCatching {
        val request = Request.Builder()
            .url("https://www.bsbsb.top/api/skipSegments?videoID=$bvid")
            .header("User-Agent", UA)
            .get()
            .build()
        val text = http.await(request).use { response ->
            if (!response.isSuccessful) return@cancellableCatching emptyList()
            response.body?.string().orEmpty()
        }
        mapSponsorSegments(text)
    }.getOrDefault(emptyList())
    sponsorSegments.put(bvid, result)
    result
}

internal suspend fun BiliApiContext.sendDanmaku(
    cid: Long,
    bvid: String,
    text: String,
    progressMs: Long = 0L,
    mode: Int = 1,
    fontSize: Int = 25,
    color: Int = 0xFFFFFF,
    aid: Long = 0,
    appearance: com.aritxonly.bdfun.core.domain.DanmakuSendAppearance = com.aritxonly.bdfun.core.domain.DanmakuSendAppearance(),
): Long = withContext(Dispatchers.IO) {
    val data = postRequest(
        "/x/v2/dm/post",
        mapOf(
            "type" to "1",
            "oid" to cid.toString(),
            "bvid" to bvid,
            "msg" to text,
            "progress" to progressMs.coerceAtLeast(0L).toString(),
            "mode" to mode.toString(),
            "fontsize" to fontSize.toString(),
            "color" to (if (appearance.vipColor) 0xFFFFFF else color).toString(),
            "pool" to "0",
            "csrf" to csrf,
        ) + buildMap {
            if (aid > 0) put("aid", aid.toString())
            if (appearance.vipColor) put("colorful", "60001")
            if (appearance.upIdentity) put("checkbox_type", "4")
        },
    )
    data.optString("dmid_str").toLongOrNull() ?: data.optLong("dmid")
}

internal suspend fun BiliApiContext.likeVideo(aid: Long, like: Boolean): Boolean = withContext(Dispatchers.IO) {
    setVideoLike(aid, if (like) 1 else 2)
}

/** 点踩 / 取消点踩：like=3 踩，4 取消踩。 */

internal suspend fun BiliApiContext.dislikeVideo(aid: Long, dislike: Boolean = true): Boolean =
    withContext(Dispatchers.IO) {
        setVideoLike(aid, if (dislike) 3 else 4)
    }

/**
 * 点赞 / 点踩统一入口。
 * 走 web 的 `archive/like`：1 点赞、2 取消赞、3 点踩、4 取消踩。
 * 旧的 `/x/v2/view/like` 已下线（404 返回 HTML，导致“解析失败”），App 端
 * `/x/v2/view/like` 只认 access_key，用网页 cookie 会返回 -101，所以这里只用 web 接口。
 */

internal suspend fun BiliApiContext.setVideoLike(aid: Long, like: Int): Boolean {
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
