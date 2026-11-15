package com.aritxonly.bdfun.core.model.pgc

/** 番剧页卡片：最近追番与推荐共用。 */
data class BangumiCard(
    val seasonId: Long,
    val title: String,
    val cover: String,
    /** 集数文案，例如「全12话」「更新至第8话」，显示在视频卡片的 UP 主位置。 */
    val indexShow: String = "",
    /** 追番数文案，例如「1234.5万追番」，显示在视频卡片的播放量位置。 */
    val followText: String = "",
    /** 需要大会员：封面右上角角标。 */
    val vip: Boolean = false,
)
