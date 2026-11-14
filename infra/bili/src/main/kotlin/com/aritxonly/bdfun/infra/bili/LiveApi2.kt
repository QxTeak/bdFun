package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveFollower
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
 *
 * 参数少了会被风控：实测只带 page 时接口返回 -400（请求错误）；
 * 补上 fnval / network / scale 后同样的 appkey+签名就能拿到 22 张卡片。
 */
internal suspend fun BiliApiContext.liveHome(page: Int = 1): LiveHome = withContext(Dispatchers.IO) {
    val result = cancellableCatching {
        val data = dataRequest(
            "/xlive/app-interface/v2/index/feed",
            liveAppParams(mapOf("page" to page.toString())),
            base = livePageBase,
            userAgent = BILI_APP_UA,
            headers = BILI_APP_HEADERS,
        )
        val list = data.optJSONArray("card_list") ?: return@cancellableCatching LiveHome()
        var areas = emptyList<LiveArea>()
        val cards = mutableListOf<LiveCard>()
        for (i in 0 until list.length()) {
            val card = list.optJSONObject(i) ?: continue
            val cardData = card.optJSONObject("card_data")
            when (card.optString("card_type")) {
                // 分区入口的 card_data 里是 { list: [...] }；关注直播走 portal（见 liveFollowers）。
                "area_entrance_v3" -> areas = cardData.cardDataList("area_entrance_v3").toLiveAreas()
                // small_card_v1 的 card_data 是「单个房间对象」，不是 list。
                // 以前按 list 解析，22 张卡全部被丢掉，页面就只剩空白。
                "small_card_v1" -> cardData?.optJSONObject("small_card_v1")?.toLiveCard()?.let(cards::add)
            }
        }
        LiveHome(emptyList(), areas, cards, hasMore = data.optInt("has_more", 0) == 1)
    }.getOrDefault(LiveHome())
    // 登录态的首页接口不一定下发 area_entrance_v3（实测 card_list 里只有 banner 和
    // small_card_v1），这时按 PiliPlus 的做法补一份分区入口，否则第二行的胶囊永远是空的。
    if (result.areas.isEmpty()) result.copy(areas = liveAreaList(0L)) else result
}

/**
 * 分区直播流。
 *
 * 原来用 app 端 `second/getList`，实测（带登录 Cookie、完整 app 参数与签名、甚至补 WBI）
 * 一直返回 -352，点了分区就是空的。BiliPai 用的 `/room/v3/area/getRoomList` 不需要签名，
 * 实测 parent_area_id=0/1/2 都能拿到 20 条，所以改走这条。
 */
internal suspend fun BiliApiContext.liveRecommend(
    page: Int = 1, parentAreaId: Long = 0L, areaId: Long = 0L,
): List<LiveCard> = withContext(Dispatchers.IO) {
    cancellableCatching {
        val data = dataRequest(
            "/room/v3/area/getRoomList",
            mapOf(
                "parent_area_id" to parentAreaId.toString(),
                "area_id" to areaId.toString(),
                "page" to page.toString(),
                "page_size" to "20",
                "sort_type" to "online",
            ),
            base = livePageBase,
            referer = liveReferer,
        )
        data.optJSONArray("list").toLiveCards()
    }.getOrDefault(emptyList())
}

/**
 * 我的关注里正在直播的 UP。
 * 和动态页顶部头像行用同一个 portal（`/x/polymer/web-dynamic/v1/portal` 的 live_users），
 * 头像、名字、房间号天然一致；未登录时接口失败，返回空列表，页面就不显示这一行。
 */
internal suspend fun BiliApiContext.liveFollowers(): List<LiveFollower> = withContext(Dispatchers.IO) {
    cancellableCatching {
        dynamicPortal().liveUsers.map { user ->
            LiveFollower(
                roomId = user.roomId,
                mid = user.mid,
                name = user.name,
                face = user.face,
                title = user.title,
            )
        }
    }.getOrDefault(emptyList())
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
            if (id <= 0L || name.isBlank()) {
                null
            } else {
                // /room/v1/Area/getList 给的是「一级分区 id」，请求时当 parent_area_id 用。
                LiveArea(id = id, name = name, parentAreaId = id, areaId = 0L)
            }
        }
    }.getOrDefault(emptyList())
}

/** 与 PiliPlus 的 liveFeedIndex / liveSecondList 完全同一套公共参数。 */
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
        "fnval" to "912",
        "https_url_req" to "1",
        "mobi_app" to "android",
        "network" to "wifi",
        "platform" to "android",
        "scale" to "2",
        "statistics" to "{\"appId\":1,\"platform\":3,\"version\":\"8.43.0\",\"abtest\":\"\"}",
    )
    params.putAll(extra)
    if (isLoggedIn) params["relation_page"] = "1"
    signAppParams(params)
    return params
}

/** card_data 里形如 { "<key>": { "list": [...] } } 的节点。 */
private fun JSONObject?.cardDataList(key: String): JSONArray? =
    this?.optJSONObject(key)?.optJSONArray("list")

private fun JSONArray?.toLiveCards(): List<LiveCard> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.toLiveCard() }
}

private fun JSONArray?.toLiveAreas(): List<LiveArea> {
    if (this == null) return emptyList()
    val areas = ArrayList<LiveArea>(length())
    for (i in 0 until length()) {
        val item = optJSONObject(i) ?: continue
        val parent = item.optLong("area_v2_parent_id", 0L)
        val area = item.optLong("area_v2_id", 0L)
        val name = item.optString("title").orEmpty().ifBlank { item.optString("name").orEmpty() }
        // 选中态需要一个唯一 key：虚拟主播这类 area_v2_id 就是 0，只能退回 parent。
        val key = if (area > 0L) area else parent
        if (key > 0L && name.isNotBlank()) {
            areas += LiveArea(id = key, name = name, parentAreaId = parent, areaId = area)
        }
    }
    return areas
}

private fun JSONObject.toLiveCard(): LiveCard? {
    val roomId = optLong("roomid", 0L).takeIf { it > 0L }
        ?: optLong("id", 0L).takeIf { it > 0L }
        ?: return null
    return LiveCard(
        roomId = roomId,
        title = optString("title").orEmpty().ifBlank { optString("uname").orEmpty() },
        uname = optString("uname").orEmpty(),
        cover = biliImage(
            optString("cover").orEmpty()
                .ifBlank { optString("system_cover").orEmpty() }
                .ifBlank { optString("room_cover").orEmpty() },
        ),
        areaName = optString("area_name").orEmpty().ifBlank { optString("parent_area_name").orEmpty() },
        watchedText = watchedText(),
    )
}

/** watched_show.text_large 才是「2163人看过」；text_small 是「2163」，只在缺字段时兜底。 */
private fun JSONObject.watchedText(): String {
    val watched = optJSONObject("watched_show")
    watched?.optString("text_large").orEmpty().takeIf { it.isNotBlank() }?.let { return it }
    watched?.optString("text_small").orEmpty().takeIf { it.isNotBlank() }?.let { return it }
    optString("text_large").takeIf { it.isNotBlank() }?.let { return it }
    val online = optLong("online", 0L)
    return if (online > 0L) "${formatLiveCount(online)}人看过" else ""
}

private fun formatLiveCount(value: Long): String = when {
    value >= 10_000L -> String.format("%.1f万", value / 10_000f)
    else -> value.toString()
}
