package com.aritxonly.bdfun.core.model.pgc

/**
 * 搜索「番剧 / 影视」Tab 的结果。
 *
 * 与 [BangumiCard] 分开：搜索接口给的是 `media_score`、`areas` 等字段，
 * 不包含追番数；点击只带 season_id 进详情页。
 */
data class SearchPgcItem(
    val seasonId: Long,
    val title: String,
    val cover: String,
    /** `season_type_name`，例如「番剧」「电影」「纪录片」。 */
    val typeName: String = "",
    val areas: String = "",
    val styles: String = "",
    val indexShow: String = "",
    val score: String = "",
    val pubdate: Long = 0L,
    val desc: String = "",
)
