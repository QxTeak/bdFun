package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.dynamicUpListParams
import com.aritxonly.bdfun.core.domain.dynamicPortalParams
import com.aritxonly.bdfun.core.domain.dynamicFeedParams
import com.aritxonly.bdfun.core.domain.DYNAMIC_UP_LIST_PATH
import com.aritxonly.bdfun.core.domain.DYNAMIC_PORTAL_PATH
import com.aritxonly.bdfun.core.domain.DYNAMIC_FEED_PATH
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

internal suspend fun BiliApiContext.userFeeds(mid: Long, offset: String = ""): FeedPage =
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
        mapFeedPage(data)
    }

/** UP 主投稿视频数。 */

internal suspend fun BiliApiContext.dynamics(): List<FeedItem> = dynamicPage("all").items

/** Hyper-PiliPlus uses the same feed for categories and a selected UP. */

internal suspend fun BiliApiContext.dynamicPage(type: String, offset: String = "", hostMid: Long? = null): FeedPage =
    withContext(Dispatchers.IO) {
        val data = try {
            dataRequest(DYNAMIC_FEED_PATH, dynamicFeedParams(type, offset, hostMid))
        } catch (error: BiliException) {
            if (error.code == 4101132) return@withContext FeedPage(emptyList(), "", false)
            throw error
        }
        mapFeedPage(data)
    }

internal suspend fun BiliApiContext.dynamicPortal(): FeedPortal = withContext(Dispatchers.IO) {
    val data = dataRequest(DYNAMIC_PORTAL_PATH, dynamicPortalParams)
    mapFeedPortal(data)
}

internal suspend fun BiliApiContext.dynamicUpList(offset: String): FeedUpPage = withContext(Dispatchers.IO) {
    parseFeedUpPage(dataRequest(DYNAMIC_UP_LIST_PATH, dynamicUpListParams(offset)))
}

/**
 * 动态详情官方接口：feed 只给摘要时，打开详情用 detail 接口拿完整的
 * opus 正文 / 图片 / 转发内容。失败返回 null，由调用方回退到列表项。
 */

internal suspend fun BiliApiContext.dynamicDetail(id: String): FeedItem? = withContext(Dispatchers.IO) {
    cancellableCatching {
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
        mapFeedDetail(data)
    }.getOrNull()
}

/** 动态点赞 / 取消。新接口要求 csrf 放 URL、正文是 JSON（form 会 4100001）。 */

internal suspend fun BiliApiContext.likeFeed(id: String, like: Boolean): Boolean = withContext(Dispatchers.IO) {
    cancellableCatching {
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
        http.await(request).use { response ->
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
        if (debug) {
            android.util.Log.w("BiliApi", "likeFeed failed id=$id like=$like", it)
        }
    }.getOrDefault(false)
}

/** 动态详情评论（type=17）。 */
