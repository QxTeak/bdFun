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
import com.aritxonly.bdfun.core.model.pgc.*
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

internal suspend fun BiliApiContext.searchSuggest(term: String): List<String> = withContext(Dispatchers.IO) {
    if (term.isBlank()) return@withContext emptyList()
    cancellableCatching {
        val root = rawRequest(
            "/main/suggest",
            mapOf(
                "term" to term,
                "main_ver" to "v1",
                "highlight" to term,
            ),
            base = "https://s.search.bilibili.com",
        )
        mapSearchSuggestions(root, ::cleanHtml)
    }.getOrDefault(emptyList())
}

/** B 站热搜词。依次尝试「搜索发现」和「热搜词」，都失败时给一份兜底列表。 */

internal suspend fun BiliApiContext.hotKeywords(): List<SearchHotWord> = withContext(Dispatchers.IO) {
    // 1) 搜索发现：data.trending.list[].keyword
    val square = cancellableCatching {
        val data = dataRequest(
            "/x/web-interface/search/square",
            mapOf("limit" to "10"),
            signed = true,
        )
        mapSearchDiscovery(data)
    }.getOrDefault(emptyList<SearchHotWord>())

    if (square.isNotEmpty()) return@withContext square

    // 2) 热搜词接口
    cancellableCatching {
        val root = rawRequest(
            path = "/main/hotword",
            params = emptyMap(),
            base = "https://s.search.bilibili.com",
        )
        mapHotwordSearch(root)
    }.getOrDefault(emptyList<SearchHotWord>())
}

/**
 * 搜索发现（对齐 PiliPlus 的 `SSearchController.queryRecommendList`）。
 *
 * 走 app.bilibili.com 的 `/x/v2/search/recommend`，参数与 PiliPlus 固定一致；
 * 注意它和 [hotKeywords] 是两份不同的数据：热搜是排行榜词，搜索发现是推荐词。
 */
internal suspend fun BiliApiContext.searchDiscovery(): List<String> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/x/v2/search/recommend",
            mapOf(
                "build" to "8430300",
                "channel" to "master",
                "version" to "8.43.0",
                "c_locale" to "zh_CN",
                "mobi_app" to "android",
                "platform" to "android",
                "s_locale" to "zh_CN",
                "from" to "2",
            ),
            base = APP,
            userAgent = APP_UA,
        )
        mapSearchRecommend(data)
    }.getOrDefault(emptyList())
}

internal suspend fun BiliApiContext.search(
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
    mapSearchVideos(data, ::cleanHtml)
}

/** 搜索用户（综合页 / 用户 Tab）。 */

internal suspend fun BiliApiContext.searchUsers(keyword: String, page: Int = 1): List<SearchUser> =
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
        mapSearchUsers(data, ::cleanHtml)
    }

/** 番剧 / 影视搜索；类型由 B 站搜索接口的 search_type 决定。 */
internal suspend fun BiliApiContext.searchPgc(
    keyword: String,
    bangumi: Boolean,
    page: Int = 1,
): List<SearchPgcItem> = withContext(Dispatchers.IO) {
    val data = dataRequest(
        "/x/web-interface/wbi/search/type",
        mapOf(
            "search_type" to if (bangumi) "media_bangumi" else "media_ft",
            "keyword" to keyword,
            "page" to page.toString(),
            "page_size" to "20",
        ),
        signed = true,
    )
    mapSearchPgc(data, ::cleanHtml)
}

internal suspend fun BiliApiContext.hotSearch(): List<String> = withContext(Dispatchers.IO) {
    val data = cancellableCatching {
        dataRequest("/x/web-interface/search/square", mapOf("limit" to "10"))
    }.getOrNull() ?: return@withContext emptyList()
    mapHotSearch(data)
}
