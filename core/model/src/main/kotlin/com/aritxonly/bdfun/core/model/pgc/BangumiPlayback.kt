package com.aritxonly.bdfun.core.model.pgc

/** 番剧的一集。字段来自 `/pgc/view/web/season` 的 episodes / section。 */
data class BangumiEpisode(
    val epId: Long,
    val aid: Long,
    val cid: Long,
    /** 这一集对应的稿件 BV 号：站内播放复用它进视频详情页。 */
    val bvid: String = "",
    val title: String,
    val longTitle: String,
    val cover: String,
    val durationMs: Long,
    /** 分组标题：正片 / 预告 / 花絮…，来自 section.title。 */
    val sectionTitle: String = "正片",
    /** 「第1话 云霄飞车杀人事件」这类展示名，接口未给时为空。 */
    val showTitle: String = "",
    val stat: BangumiStat = BangumiStat(),
    /** 发布时间（epoch 秒）。 */
    val pubTime: Long = 0L,
) {
    /** 选集按钮上显示的短名：优先接口的 show_title。 */
    val displayName: String get() = showTitle.ifBlank { if (longTitle.isBlank()) title else longTitle }
}

/** 番剧统计；字段名来自 `/pgc/view/web/season` 的 stat 对象（单集 stat 也用同一形状）。 */
data class BangumiStat(
    val play: Long = 0L,
    val danmaku: Long = 0L,
    val like: Long = 0L,
    val coin: Long = 0L,
    val favorite: Long = 0L,
    val share: Long = 0L,
    val reply: Long = 0L,
)

/** 出品方信息（up_info）。 */
data class BangumiUpInfo(
    val mid: Long = 0L,
    val name: String = "",
    val avatar: String = "",
)

/** 当前账号与这部番的关系（user_status）。 */
data class BangumiUserStatus(
    val followed: Boolean = false,
    /** 上次看到哪一集；未登录/未看过为 0。 */
    val lastEpId: Long = 0L,
)

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
    /** 1 番剧 / 2 电影 / 3 纪录片 / 4 国创 / 5 电视剧 / 7 综艺。 */
    val type: Int = 0,
    val stat: BangumiStat = BangumiStat(),
    val up: BangumiUpInfo = BangumiUpInfo(),
    val userStatus: BangumiUserStatus = BangumiUserStatus(),
) {
    val sections: List<Pair<String, List<BangumiEpisode>>>
        get() = episodes.groupBy { it.sectionTitle }.map { (title, list) -> title to list }
}
