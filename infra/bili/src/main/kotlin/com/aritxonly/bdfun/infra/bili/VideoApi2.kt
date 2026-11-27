package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.mapper.readVideoDetail
import com.aritxonly.bdfun.infra.bili.mapper.mapVideoDetail
import com.aritxonly.bdfun.core.domain.playbackCodecPriority
import com.aritxonly.bdfun.core.domain.playbackCodecMatches
import com.aritxonly.bdfun.core.domain.choosePlaybackVideo
import com.aritxonly.bdfun.core.domain.choosePlaybackAudio
import com.aritxonly.bdfun.core.model.video.VideoItemExtras
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

internal suspend fun BiliApiContext.coinVideo(aid: Long, count: Int, alsoLike: Boolean): Boolean =
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

internal suspend fun BiliApiContext.triple(aid: Long): Boolean = withContext(Dispatchers.IO) {
    postRequest(
        "/x/web-interface/archive/like/triple",
        mapOf("aid" to aid.toString(), "csrf" to csrf),
    )
    true
}

/** 该视频已收藏在哪些收藏夹里（用于判断取消）。接口异常时返回空集合。 */

internal suspend fun BiliApiContext.onlineTotal(aid: Long, cid: Long): Pair<Long, String> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/x/player/online/total",
                mapOf("aid" to aid.toString(), "cid" to cid.toString()),
            )
            mapVideoOnline(data)
        }.getOrDefault(0L to "")
    }

/** 视频标签：话题/BGM/普通标签，接口与 PiliPlus 的 videoTags 相同。 */
internal suspend fun BiliApiContext.videoTags(bvid: String): List<VideoTag> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val root = rawRequest(
            "/x/web-interface/view/detail/tag",
            mapOf("bvid" to bvid),
        )
        mapVideoTags(root.optJSONArray("data"))
    }.getOrDefault(emptyList())
}

internal suspend fun BiliApiContext.subtitles(aid: Long, cid: Long): List<SubtitleTrack> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/player/v2",
            mapOf("aid" to aid.toString(), "cid" to cid.toString()),
        )
        mapSubtitleTracks(data)
    }.getOrDefault(emptyList())
}

/** 下载并解析字幕 JSON（B 站字幕是 `body[]` 的 from/to/content）。 */

internal suspend fun BiliApiContext.subtitleCues(url: String): List<SubtitleCue> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val request = Request.Builder()
            .url(if (url.startsWith("//")) "https:$url" else url)
            .header("User-Agent", UA)
            .header("Referer", "https://www.bilibili.com/")
            .get()
            .build()
        val text = http.await(request).use { response ->
            if (!response.isSuccessful) return@cancellableCatching emptyList()
            response.body?.string().orEmpty()
        }
        mapSubtitleCues(text)
    }.getOrDefault(emptyList())
}

internal fun BiliApiContext.parseSpaceVideo(item: JSONObject): VideoItem? = mapSpaceVideo(item, ::cleanHtml)

internal fun BiliApiContext.parseVideoDetail(data: JSONObject): VideoDetail =
    mapVideoDetail(readVideoDetail(data), ::cleanHtml)

/** 网络类型、VPN、系统代理：用来判断是不是代理/VPN 把 playurl 换成了空内容。 */

internal fun BiliApiContext.netInfo(): String {
    val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE)
        as? android.net.ConnectivityManager
    val caps = cm?.activeNetwork?.let { cm.getNetworkCapabilities(it) }
    val transports = buildList {
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cell")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET) == true) add("ethernet")
        if (caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
    }.joinToString("+").ifBlank { "none" }
    val proxy = cancellableCatching {
        java.net.ProxySelector.getDefault()
            ?.select(java.net.URI("https://api.bilibili.com"))
            ?.firstOrNull()
            ?.toString()
    }.getOrNull()
    return "net=$transports vpn=${caps?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN)} " +
        "proxy=$proxy"
}

internal fun BiliApiContext.parsePlayInfo(
    data: JSONObject, preferred: Int, detail: VideoDetail, durationSec: Int = detail.duration,
    codecPreference: String = "auto", cdnService: String = "base",
): PlayInfo {
    val cdn = cdnServiceById(cdnService)
    return mapPlayInfo(readPlayInfo(data), durationSec, codecPreference) { url, backups -> cdn.rewrite(url, backups) }
}
