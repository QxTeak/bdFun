package com.aritxonly.bdfun.infra.bili

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

internal class BiliApiContext(
    context: Context,
    internal val debug: Boolean,
    internal val log: (String, String) -> Unit,
    internal val cdnResolver: (String) -> CdnService,
) {
    internal val appContext: Context = context.applicationContext
    val cookieJar = BiliCookieJar(context)

    internal val client = HttpClients.base.newBuilder()
        .cookieJar(cookieJar)
        .build()

    val webSocketClient = HttpClients.base.newBuilder().cookieJar(cookieJar).build()

    internal val http = com.aritxonly.bdfun.infra.bili.BiliHttpClient(
        client = client,
        debug = debug,
        log = log,
    )

    internal val imHttp = BiliHttpClient(
        client = com.aritxonly.bdfun.infra.bili.message.imTransportClient(client),
        debug = debug,
        log = log,
    )

    internal val buvidMutex = Mutex()
    internal val wbiSigner = com.aritxonly.bdfun.infra.bili.WbiSigner(
        loadKeys = {
            val root = rawRequest("/x/web-interface/nav", emptyMap())
            root.optJSONObject("data")?.optJSONObject("wbi_img")?.let { wbi ->
                wbi.s("img_url").substringAfterLast('/').substringBefore('.') to
                    wbi.s("sub_url").substringAfterLast('/').substringBefore('.')
            }
        },
        encode = ::encode,
    )
    internal val appSigner = com.aritxonly.bdfun.infra.bili.AppRequestSigner(::encode)

    internal val requests by lazy {
        com.aritxonly.bdfun.infra.bili.BiliRequestGateway(
            http = http,
            beforeRequest = { ensureBuvid() },
            signQuery = wbiSigner::signQuery,
            refreshSigningKeys = { wbiSigner.forceRefresh() },
            encode = ::encode,
            debug = debug,
            apiBase = API,
            passportBase = PASSPORT,
            defaultUserAgent = UA,
        )
    }

    /** 消息会话昵称/头像后台补全：不阻塞首屏列表。 */
    internal val backgroundScope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + Dispatchers.IO,
    )
    /** 历史记录补播放量/弹幕数的缓存：bvid -> (play, danmaku)，最多 300 条。 */
    internal val videoStatCache = object : LinkedHashMap<String, Pair<Long, Long>>(300, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, Pair<Long, Long>>?,
        ): Boolean = size > 300
    }

    internal fun cachedVideoStat(bvid: String): Pair<Long, Long>? =
        synchronized(videoStatCache) { videoStatCache[bvid] }

    internal fun putVideoStat(bvid: String, stat: Pair<Long, Long>) {
        synchronized(videoStatCache) { videoStatCache[bvid] = stat }
    }

    /** 消息卡片补封面 / UP 主用的小缓存：bvid -> (封面, UP 主 mid)。 */

    internal val messageVideoCache =
        object : LinkedHashMap<String, MessageVideoBrief>(64, 0.75f, true) {
            override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<String, MessageVideoBrief>?,
            ): Boolean = size > 64
        }

    /** buvid 获取失败后的冷却时间，避免每个请求都重新卡在一次失败请求上。 */
    @Volatile
    internal var buvidRetryAt = 0L

    val isLoggedIn: Boolean get() = cookieJar.hasSession
    val mid: Long get() = cookieJar.mid
    val csrf: String get() = cookieJar.csrf

    internal fun cdnServiceById(id: String) = cdnResolver(id)
    internal val messageUsers = MessageUserCache()
    internal val sponsorSegments = SponsorSegmentCache()
}

internal data class MessageVideoBrief(val cover: String, val ownerMid: Long)
