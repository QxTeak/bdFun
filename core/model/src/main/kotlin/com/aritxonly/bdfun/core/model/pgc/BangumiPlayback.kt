package com.aritxonly.bdfun.core.model.pgc

/** 番剧的一集。字段来自 `/pgc/view/web/season` 的 episodes / sections。 */
data class BangumiEpisode(
    val epId: Long,
    val aid: Long,
    val cid: Long,
    /** 这一集对应的稿件 BV 号：站内播放复用它进视频详情页。 */
    val bvid: String = "",
    val title: String,
    val longTitle: String,
    val cover: String,
    /** 集号文本，如「1」「12」。 */
    val index: String,
    val durationMs: Long,
    /** 分组标题：正片 / 预告 / 花絮…，来自 sections.title。 */
    val sectionTitle: String = "正片",
) {
    /** 选集按钮上显示的短名：有长标题用集号+长标题，没有就用 title。 */
    val displayName: String get() = if (longTitle.isBlank()) title else longTitle
}

/** 一部番剧的基本信息 + 全部剧集。 */
data class BangumiSeason(
    val seasonId: Long,
    val title: String,
    val cover: String,
    /** 评分文本，如 9.5；无评分是空串。 */
    val rating: String,
    val evaluate: String,
    val subtitle: String,
    val episodes: List<BangumiEpisode>,
) {
    val sections: List<Pair<String, List<BangumiEpisode>>>
        get() = episodes.groupBy { it.sectionTitle }.map { (title, list) -> title to list }
}

/** 播放地址：DASH 时音视频分开，durl 时只有一路。 */
data class BangumiStream(
    val videoUrl: String,
    val audioUrl: String?,
    val qualityId: Int,
    val qualityLabel: String,
    val durationMs: Long,
)
