package com.aritxonly.bdfun.core.model.pgc

/**
 * 番剧排行榜的一个榜单分类，来自 /pgc/season/rank/tabs。
 *
 * [oid] 是接口原样下发的 `{season_type:1,style_id:70}` 这种串，**不是合法 JSON**；
 * 请求榜单内容时必须原样回传，不要解析、不要重新拼。
 */
data class BangumiRankTab(
    val rankId: Long,
    val title: String,
    val oid: String,
)

/** 榜单里的一条番剧。 */
data class BangumiRankEntry(
    val rank: Int,
    val seasonId: Long,
    val title: String,
    val cover: String,
    /** 卡片标题下面那行推荐语，接口字段 subtitle。 */
    val subtitle: String = "",
    /** 「更新至第X话 / 全X话」，接口字段 new_ep.index_show。 */
    val indexShow: String = "",
    /** 评分文本，空串表示暂无评分。 */
    val rating: String = "",
    /** 样式标签，如「番剧·漫画改·智斗」。 */
    val style: String = "",
    /** 冲榜文案，如「荣登榜首」，常为空。 */
    val tag: String = "",
    /** 需要大会员。榜单接口不返回，由网页榜单的 badge 对照补上。 */
    val vip: Boolean = false,
)

/** 一个榜单的完整内容；接口一次性返回整榜，不分页。 */
data class BangumiRankBoard(
    val note: String = "",
    val entries: List<BangumiRankEntry> = emptyList(),
)
