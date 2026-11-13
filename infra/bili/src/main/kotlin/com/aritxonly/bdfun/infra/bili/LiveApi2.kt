package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveHome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private const val livePageBase = "https://api.live.bilibili.com"
private const val liveReferer = "https://live.bilibili.com/"

/**
 * 直播首页，照搬 PiliPlus 的 `LiveHttp.liveFeedIndex`：
 * app 端 `/xlive/app-interface/v2/index/feed`，必须 AppSign，web 的 WBI 在这条接口上无效。
 */
internal suspend fun BiliApiContext.liveHome(page: Int = 1): LiveHome = withContext(Dispatchers.IO) {
    val result = cancellableCatching {
        val params = liveAppParams(mapOf("page" to page.toString()))
        val data = dataRequest(
            "/xlive/app-interface/v2/index/feed",
            params,
            base = livePageBase,
            userAgent = BILI_APP_UA,
            headers = BILI_APP_HEADERS,
        )
        val list = data.optJSONArray("card_list") ?: return@cancellableCatching LiveHome()
        var following = emptyList<LiveCard>()
        var areas = emptyList<LiveArea>()
        val cards = mutableListOf<LiveCard>()
        for (i in 0 until list.length()) {
            val card = list.optJSONObject(i) ?: continue
            val cardData = card.optJSONObject("card_data")
            when (card.optString("card_type")) {
                "my_idol_v1" -> following = cardData.parseLiveCardList("my_idol_v1")
                "area_entrance_v3" -> areas = cardData.parseLiveAreas("area_entrance_v3")
                "small_card_v1" -> cards += cardData.parseLiveCardList("small_card_v1")
            }
        }
        LiveHome(following, areas, cards)
    }.getOrDefault(LiveHome())
    // app 端整段拿不到时（签名被拒/接口调整），退回 web 分区列表，至少页面不是空的。
    if (result.cards.isNotEmpty() || result.areas.isNotEmpty()) {
        result
    } else {
        LiveHome(
            following = result.following,
            areas = liveAreaList(0L),
            cards = webLiveList(1, 0L, 0L),
        )
    }
}

/** 分区直播流，照搬 PiliPlus 的 `LiveHttp.liveSecondList`（app 端 second/getList）。 */
internal suspend fun BiliApiContext.liveRecommend(
    page: Int = 1, parentAreaId: Long = 0L, areaId: Long = 0L,
): List<LiveCard> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val params = liveAppParams(
            mapOf(
                "area_id" to areaId.toString(),
                "parent_area_id" to parentAreaId.toString(),
                "page" to page.toString(),
                "page_size" to "20",
                "tag_version" to "1",
                "qn" to "0",
                "sort_type" to "online",
            ),
        )
        val data = dataRequest(
            "/xlive/app-interface/v2/second/getList",
            params,
            base = livePageBase,
            userAgent = BILI_APP_UA,
            headers = BILI_APP_HEADERS,
        )
        data.optJSONArray("list").toLiveCards()
    }.getOrDefault(emptyList()).ifEmpty { webLiveList(page, parentAreaId, areaId) }
}

/** App 端接口异常时的兜底：老的 web 分区列表（有 buvid 时会话通常能过）。 */
private suspend fun BiliApiContext.webLiveList(
    page: Int, parentAreaId: Long, areaId: Long,
): List<LiveCard> {
    val params = mapOf(
        "platform" to "web",
        "parent_area_id" to parentAreaId.toString(),
        "area_id" to areaId.toString(),
        "sort_type" to "online",
        "page" to page.toString(),
        "page_size" to "20",
    )
    suspend fun once(signed: Boolean): List<LiveCard> = cancellableCatching {
        val data = dataRequest(
            "/xlive/web-interface/v1/second/getList",
            params,
            signed = signed,
            base = livePageBase,
            referer = liveReferer,
        )
        data.optJSONArray("list").toLiveCards()
    }.getOrDefault(emptyList())
    return once(true).ifEmpty { once(false) }
}

/** 一级分区兜底列表（卡片里没有 area_entrance_v3 时用）。 */
internal suspend fun BiliApiContext.liveAreaList(parentId: Long = 0L): List<LiveArea> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val root = rawRequest(
            "/room/v1/Area/getList",
            mapOf("parent_id" to parentId.toString()),
            base = livePageBase,
            referer = liveReferer,
        )
        val list = root.optJSONArray("data") ?: return@cancellableCatching emptyList()
        (0 until list.length()).mapNotNull { i ->
            val item = list.optJSONObject(i) ?: return@mapNotNull null
            val id = item.optLong("id", 0L)
            val name = item.optString("name").orEmpty()
            if (id <= 0L || name.isBlank()) null else LiveArea(id, name, parentId)
        }
    }.getOrDefault(emptyList())
}

private fun BiliApiContext.liveAppParams(extra: Map<String, String>): MutableMap<String, String> {
    val params = mutableMapOf(
        "channel" to "master",
        "actionKey" to "appkey",
        "build" to "8430300",
        "version" to "8.43.0",
        "c_locale" to "zh_CN",
        "s_locale" to "zh_CN",
        "device" to "android",
        "device_name" to "android",
        "device_type" to "0",
        "disable_rcmd" to "0",
        "https_url_req" to "1",
        "mobi_app" to "android",
        "platform" to "android",
        "statistics" to "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
    )
    params.putAll(extra)
    if (isLoggedIn) params["relation_page"] = "1"
    signAppParams(params)
    return params
}

private fun JSONObject?.parseLiveCardList(key: String): List<LiveCard> =
    this?.optJSONObject(key)?.optJSONArray("list").toLiveCards()

private fun JSONObject?.parseLiveAreas(key: String): List<LiveArea> {
    val list = this?.optJSONObject(key)?.optJSONArray("list") ?: return emptyList()
    val areas = ArrayList<LiveArea>(list.length())
    for (i in 0 until list.length()) {
        val item = list.optJSONObject(i) ?: continue
        val parent = item.optLong("area_v2_parent_id", 0L)
        val area = item.optLong("area_v2_id", 0L)
        val title = item.optString("title").orEmpty()
        // 分区入口以 parent 为主键；像「颜值」这种带 area_id 的用 area 做 key。
        val id = if (area > 0L) area else parent
        if (id > 0L && title.isNotBlank()) areas += LiveArea(id, title, parent)
    }
    return areas
}

private fun JSONArray?.toLiveCards(): List<LiveCard> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.toLiveCard() }
}

private fun JSONObject.toLiveCard(): LiveCard? {
    val roomId = optLong("roomid", 0L).takeIf { it > 0L }
        ?: optLong("id", 0L).takeIf { it > 0L }
        ?: return null
    val watched = optJSONObject("watched_show")?.optString("text_small").orEmpty()
        .ifBlank { optString("text_small").orEmpty() }
        .ifBlank {
            val online = optLong("online", 0L)
            if (online > 0L) "${formatLiveCount(online)}人看过" else ""
        }
    val cover = optString("cover").orEmpty()
        .ifBlank { optString("system_cover").orEmpty() }
        .ifBlank { optString("room_cover").orEmpty() }
    return LiveCard(
        roomId = roomId,
        title = optString("title").orEmpty().ifBlank { optString("uname").orEmpty() },
        uname = optString("uname").orEmpty(),
        cover = biliImage(cover),
        areaName = optString("area_name").orEmpty(),
        watchedText = watched,
    )
}

private fun formatLiveCount(value: Long): String = when {
    value >= 10_000L -> String.format("%.1f万", value / 10_000f)
    else -> value.toString()
}
