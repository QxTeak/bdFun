package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val pgcReferer = "https://www.bilibili.com/anime/"

/**
 * 最近追番（PiliPlus 的「我的追番」接口 /x/space/bangumi/follow/list?type=1）。
 * 未登录返回空列表，页面只显示推荐。
 */
internal suspend fun BiliApiContext.pgcFollowing(page: Int = 1): List<BangumiCard> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/x/space/bangumi/follow/list",
                mapOf(
                    "type" to "1",
                    "pn" to page.toString(),
                    "ps" to "15",
                ),
                referer = pgcReferer,
            )
            val list = data.optJSONArray("list") ?: return@cancellableCatching emptyList()
            (0 until list.length()).mapNotNull { i ->
                list.optJSONObject(i)?.toBangumiCard()
            }
        }.getOrDefault(emptyList())
    }

/**
 * 番剧推荐（PiliPlus 番剧索引同款 /pgc/season/index/result）。
 * order=3 为「追番人数」排序，接口因此会带回 order 字段（xx万追番）。
 */
internal suspend fun BiliApiContext.pgcIndex(page: Int = 1, pageSize: Int = 20): List<BangumiCard> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/pgc/season/index/result",
                mapOf(
                    "st" to "1",
                    "order" to "3",
                    "sort" to "0",
                    "season_type" to "1",
                    "type" to "1",
                    "season_version" to "-1",
                    "spoken_language_type" to "-1",
                    "area" to "-1",
                    "is_finish" to "-1",
                    "copyright" to "-1",
                    "season_status" to "-1",
                    "season_month" to "-1",
                    "year" to "-1",
                    "style_id" to "-1",
                    "page" to page.toString(),
                    "pagesize" to pageSize.toString(),
                ),
                referer = pgcReferer,
            )
            val list = data.optJSONArray("list") ?: return@cancellableCatching emptyList()
            (0 until list.length()).mapNotNull { i ->
                list.optJSONObject(i)?.toBangumiCard()
            }
        }.getOrDefault(emptyList())
    }

private fun JSONObject.toBangumiCard(): BangumiCard? {
    val seasonId = optLong("season_id", 0L).takeIf { it > 0L } ?: return null
    val badge = optString("badge").orEmpty()
    return BangumiCard(
        seasonId = seasonId,
        title = optString("title").orEmpty(),
        cover = biliImage(optString("cover").orEmpty()),
        indexShow = optString("index_show").orEmpty().ifBlank {
            optJSONObject("new_ep")?.optString("index_show").orEmpty()
        },
        followText = optString("order").orEmpty().takeIf { it.isNotBlank() }?.let { "$it追番" }.orEmpty(),
        vip = badge.contains("会员"),
    )
}
