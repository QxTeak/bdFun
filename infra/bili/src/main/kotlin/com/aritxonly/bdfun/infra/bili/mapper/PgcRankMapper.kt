package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.common.biliImageThumbUrl
import com.aritxonly.bdfun.core.model.pgc.BangumiRankBoard
import com.aritxonly.bdfun.core.model.pgc.BangumiRankEntry
import com.aritxonly.bdfun.core.model.pgc.BangumiRankTab
import org.json.JSONArray
import org.json.JSONObject

/**
 * 榜单目录：`data` 是数组，每项 {oid, rank_id, rank_title}。
 *
 * 真实样本见 D:\work\_apicollect\samples\rank_tabs_index1.json。
 */
internal fun mapRankTabs(data: JSONArray?): List<BangumiRankTab> =
    data.objects().mapNotNull { item ->
        val rankId = item.l("rank_id")
        if (rankId <= 0L) return@mapNotNull null
        BangumiRankTab(
            rankId = rankId,
            title = item.s("rank_title"),
            oid = item.s("oid"),
        )
    }

/**
 * 榜单内容：`data` 是对象，条目在 season_vo_list。
 *
 * rating / subtitle / season_tag 都可能是空串；new_ep 可能整个缺失。
 * 真实样本见 samples\rank_list_rank124_top3.json。
 */
internal fun mapRankBoard(data: JSONObject): BangumiRankBoard = BangumiRankBoard(
    note = data.s("note"),
    entries = data.optJSONArray("season_vo_list").objects().mapNotNull { it.toRankEntry() },
)

/**
 * 网页版榜单里的会员角标 → 需要大会员的 season_id 集合。
 * 榜单接口本身没有这个信息，只能靠它补 `vip`。
 */
internal fun mapRankVipSeasons(payload: JSONObject): Set<Long> {
    val list = payload.optJSONArray("list") ?: return emptySet()
    val vip = HashSet<Long>()
    list.objects().forEach { item ->
        val badge = item.s("badge") + item.optJSONObject("badge_info")?.s("text").orEmpty()
        if (badge.contains("会员")) {
            val seasonId = item.l("season_id")
            if (seasonId > 0L) vip += seasonId
        }
    }
    return vip
}

private fun JSONObject.toRankEntry(): BangumiRankEntry? {    val seasonId = l("season_id")
    if (seasonId <= 0L) return null
    val cover = s("cover").ifBlank { s("ss_horizontal_cover") }
    return BangumiRankEntry(
        rank = optInt("rank", 0),
        seasonId = seasonId,
        title = s("title"),
        // 必须用只缩宽、不裁切的地址：`@480w_270h_1c` 会让服务端先裁成 16:9，
        // 卡片再裁到 3:4 就是裁两遍，人像会被切头。
        cover = biliImageThumbUrl(cover, 480),
        subtitle = s("subtitle"),
        indexShow = optJSONObject("new_ep")?.s("index_show").orEmpty(),
        rating = s("rating"),
        style = s("season_style").removePrefix("番剧·").removePrefix("国创·"),
        tag = s("season_tag"),
    )
}
