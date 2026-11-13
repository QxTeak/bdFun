package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val livePageBase = "https://api.live.bilibili.com"
private const val liveReferer = "https://live.bilibili.com/"

/**
 * 关注的正在直播（PiliPlus 同款接口 /xlive/web-ucenter/user/following）。
 * 未登录或被风控时返回空列表，页面隐藏这一行。
 */
internal suspend fun BiliApiContext.liveFollowing(page: Int = 1): List<LiveCard> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/xlive/web-ucenter/user/following",
                mapOf(
                    "page" to page.toString(),
                    "page_size" to "9",
                    "ignoreRecord" to "1",
                    "hit_ab" to "true",
                ),
                base = livePageBase,
                referer = liveReferer,
            )
            val list = data.optJSONArray("list") ?: return@cancellableCatching emptyList()
            (0 until list.length()).mapNotNull { i ->
                val item = list.optJSONObject(i) ?: return@mapNotNull null
                if (item.optInt("live_status", 0) != 1) return@mapNotNull null
                item.toLiveCard()
            }
        }.getOrDefault(emptyList())
    }

/**
 * 推荐直播（沿用首页已有的 web 接口 /second/getList）。
 * parentAreaId / areaId 传 0 表示全部；切分区时传对应 id。
 */
internal suspend fun BiliApiContext.liveRecommend(
    page: Int = 1, parentAreaId: Long = 0L, areaId: Long = 0L,
): List<LiveCard> = withContext(Dispatchers.IO) {
    // 这个 web 接口现在不带 WBI 会直接 -352（风控），和 getDanmuInfo 一样：
    // 先签名请求，失败就清一次密钥再签一遍。
    val params = mapOf(
        "platform" to "web",
        "parent_area_id" to parentAreaId.toString(),
        "area_id" to areaId.toString(),
        "sort_type" to "online",
        "page" to page.toString(),
        "page_size" to "20",
    )
    suspend fun load(signed: Boolean): List<LiveCard> = cancellableCatching {
        val data = dataRequest(
            "/xlive/web-interface/v1/second/getList",
            params,
            signed = signed,
            base = livePageBase,
            referer = liveReferer,
        )
        val list = data.optJSONArray("list") ?: return@cancellableCatching emptyList()
        (0 until list.length()).mapNotNull { i -> list.optJSONObject(i)?.toLiveCard() }
    }.getOrDefault(emptyList())

    load(signed = true).ifEmpty { load(signed = false) }.ifEmpty {
        wbiSigner.clear()
        load(signed = true)
    }
}

/** 直播分区：web 端 /room/v1/Area/getList，data 本身是数组。parentId=0 取一级分区。 */
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

private fun JSONObject.toLiveCard(): LiveCard? {
    val roomId = optLong("roomid", 0L).takeIf { it > 0L } ?: return null
    val watched = optJSONObject("watched_show")?.optString("text_small").orEmpty()
        .ifBlank { optString("text_small").orEmpty() }
        .ifBlank {
            val online = optLong("online", 0L)
            if (online > 0L) "${formatLiveCount(online)}人看过" else ""
        }
    return LiveCard(
        roomId = roomId,
        title = optString("title").orEmpty().ifBlank { optString("uname").orEmpty() },
        uname = optString("uname").orEmpty(),
        cover = biliImage(optString("cover").orEmpty().ifBlank { optString("room_cover").orEmpty() }),
        areaName = optString("area_name").orEmpty(),
        watchedText = watched,
    )
}

private fun formatLiveCount(value: Long): String = when {
    value >= 10_000L -> String.format("%.1f万", value / 10_000f)
    else -> value.toString()
}
