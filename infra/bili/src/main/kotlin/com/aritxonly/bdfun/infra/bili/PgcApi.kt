package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.infra.bili.mapper.mapPgcTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val pgcReferer = "https://www.bilibili.com/anime/"

/**
 * 最近追番（PiliPlus 的「我的追番」接口 /x/space/bangumi/follow/list）。
 * type=1 番剧、type=2 影视；未登录返回空列表，页面只显示推荐。
 */
internal suspend fun BiliApiContext.pgcFollowing(page: Int = 1, type: Int = 1): List<BangumiCard> =
    withContext(Dispatchers.IO) {
        // 这个接口必须带自己的 mid；未登录时直接返回空，别把 -101 当成页面错误。
        if (!isLoggedIn || mid <= 0L) return@withContext emptyList()
        cancellableCatching {
            val data = dataRequest(
                "/x/space/bangumi/follow/list",
                mapOf(
                    "vmid" to mid.toString(),
                    "type" to type.toString(),
                    "follow_status" to "0",
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
 * 番剧/影视推荐（PiliPlus 番剧索引同款 /pgc/season/index/result）。
 * order=3 为「追番人数」排序，接口因此会带回 order 字段（xx万追番）。
 * seasonType：1 番剧、2 电影、5 电视剧、3 纪录片、7 综艺。
 */
internal suspend fun BiliApiContext.pgcIndex(
    page: Int = 1,
    pageSize: Int = 20,
    seasonType: Int = 1,
): List<BangumiCard> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val data = dataRequest(
                "/pgc/season/index/result",
                mapOf(
                    "st" to "1",
                    "order" to "3",
                    "sort" to "0",
                    "season_type" to seasonType.toString(),
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

/**
 * 番剧时间表（正在更新的新番）：types=1 番剧、4 国创。
 * 头图用它替换旧榜单封面；接口是 `result` 数组而不是 `data`，走 rawRequest 自己取。
 */
internal suspend fun BiliApiContext.pgcTimeline(types: Int): List<BangumiCard> =
    withContext(Dispatchers.IO) {
        cancellableCatching {
            val root = rawRequest(
                "/pgc/web/timeline",
                mapOf("types" to types.toString(), "before" to "6", "after" to "6"),
                referer = pgcReferer,
            )
            val code = root.optInt("code", 0)
            if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
            mapPgcTimeline(root, System.currentTimeMillis() / 1000L)
        }.getOrDefault(emptyList())
    }

private fun JSONObject.toBangumiCard(): BangumiCard? {
    val seasonId = optLong("season_id", 0L).takeIf { it > 0L } ?: return null
    val badge = optString("badge").orEmpty()
    return BangumiCard(
        seasonId = seasonId,
        title = optString("title").orEmpty(),
        // 和榜单区同一档 480w，避免推荐/追番用原图造成滚动解码压力。
        cover = biliImageThumbUrl(optString("cover").orEmpty(), 480),
        indexShow = optString("index_show").orEmpty().ifBlank {
            optJSONObject("new_ep")?.optString("index_show").orEmpty()
        },
        // order 已经是「1770.2万追番」这种成品文案，别再拼一次后缀；影视还会返回「xx万追剧」。
        followText = optString("order").orEmpty().let { raw ->
            when {
                raw.isBlank() -> ""
                raw.contains("追番") || raw.contains("追剧") -> raw
                else -> "${raw}追番"
            }
        },
        vip = badge.contains("会员"),
        badge = badge.trim(),
    )
}
