package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.mapper.readFeedVideo
import com.aritxonly.bdfun.infra.bili.mapper.mapFeedVideo
import com.aritxonly.bdfun.infra.bili.mapper.readPopularPage
import com.aritxonly.bdfun.core.model.video.PopularPage
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

internal suspend fun BiliApiContext.recommend(freshIndex: Int = 1, web: Boolean = true): List<VideoItem> = withContext(Dispatchers.IO) {
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
        log(
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
        log(
            "FEED",
            "source=${if (web) "web->app-fallback" else "app->web-fallback"} " +
                "count=${fallback.size} first=${fallback.firstOrNull()?.bvid}",
        )
        return@withContext fallback
    }
    if (primaryError != null) throw primaryError
    emptyList()
}

internal suspend fun BiliApiContext.webRecommend(freshIndex: Int): List<VideoItem> {
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
    return mapFeedVideos(data, "item", this@webRecommend::parseFeedVideo)
}

internal suspend fun BiliApiContext.appRecommend(freshIndex: Int): List<VideoItem> {
    // App 推荐域名是 app.bilibili.com；用 api.bilibili.com 会 404，随后悄悄退回 Web 推荐。
    // 参数与请求头按 PiliPlus 的 rcmdVideoListApp 补齐：缺 device/fnval/flush 等字段时，
    // 服务端会走降级推荐池，返回的内容明显更旧更窄。
    val data = dataRequest(
        "/x/v2/feed/index",
        mapOf(
            "build" to "8430300",
            "c_locale" to "zh_CN",
            "channel" to "master",
            "column" to "2",
            "device" to "phone",
            "device_name" to "android",
            "device_type" to "0",
            "disable_rcmd" to "0",
            "flush" to "8",
            "fnval" to "976",
            "fnver" to "0",
            "force_host" to "2",
            "fourk" to "1",
            "guidance" to "1",
            "https_url_req" to "1",
            "idx" to freshIndex.toString(),
            "login_event" to "0",
            "mobi_app" to "android_i",
            "network" to "wifi",
            "platform" to "android",
            "player_net" to "1",
            "ps" to "12",
            "pull" to if (freshIndex == 0) "true" else "false",
            "qn" to "32",
            "recsys_mode" to "0",
            "s_locale" to "zh_CN",
            "splash_id" to "",
            "statistics" to BILI_APP_STATISTICS,
            "style" to "2",
            "voice_balance" to "0",
        ),
        base = APP,
        userAgent = BILI_APP_HD_UA,
        headers = appRecommendHeaders(),
    )
    val mapped = mapAppRecommendations(data, ::cleanHtml)
    log("FEED", "app-raw=${mapped.rawCount} kept=${mapped.items.size}")
    return mapped.items
}

/** 设备指纹类请求头：buvid 取本地 cookie，其余字段与 PiliPlus 固定值一致。 */
internal fun BiliApiContext.appRecommendHeaders(): Map<String, String> = mapOf(
    "buvid" to cookieJar.value("buvid3"),
    "fp_local" to APP_RECOMMEND_FP,
    "fp_remote" to APP_RECOMMEND_FP,
    "session_id" to "11111111",
    "env" to "prod",
    "app-key" to "android_hd",
    "x-bili-trace-id" to BILI_APP_TRACE_ID,
    "x-bili-aurora-eid" to "",
    "x-bili-aurora-zone" to "",
    "bili-http-engine" to "cronet",
)

private const val APP_RECOMMEND_FP =
    "1111111111111111111111111111111111111111111111111111111111111111"

internal suspend fun BiliApiContext.dislikeRecommendation(video: VideoItem, reason: RecommendationReason) {
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

/**
 * 撤销 App 推荐的“不感兴趣”反馈；参数与提交时一致（reason_id 也要带上），
 * 否则服务端会按“缺少来源反馈”返回参数错误。
 */
internal suspend fun BiliApiContext.cancelDislikeRecommendation(
    video: VideoItem, reason: RecommendationReason,
) {
    check(cookieJar.value("access_key").isNotBlank()) { "请重新扫码登录以撤销推荐反馈" }
    val params = mutableMapOf(
        "goto" to "av", "id" to video.aid.toString(),
        "reason_id" to reason.id.toString(),
        "build" to "1", "mobi_app" to "android",
    )
    signAppParams(params)
    dataRequest("/x/feed/dislike/cancel", params, base = APP)
}

internal suspend fun BiliApiContext.popular(page: Int = 1): PopularPage = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/popular",
        mapOf("pn" to page.toString(), "ps" to "20"),
    )
    readPopularPage(data)
}

internal suspend fun BiliApiContext.ranking(rid: Int = 0): List<VideoItem> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/ranking/v2",
        mapOf("rid" to rid.toString(), "type" to "all"),
    )
    mapFeedVideos(data, "list", this@ranking::parseFeedVideo)
}

// ---------------------------------------------------------------- video

internal fun BiliApiContext.parseFeedVideo(item: JSONObject): VideoItem? =
    readFeedVideo(item)?.let { mapFeedVideo(it, ::cleanHtml) }
