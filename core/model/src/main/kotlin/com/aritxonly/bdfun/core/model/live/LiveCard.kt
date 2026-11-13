package com.aritxonly.bdfun.core.model.live

/** 直播页卡片：关注列表与分区列表共用。 */
data class LiveCard(
    val roomId: Long,
    val title: String,
    val uname: String,
    val cover: String,
    /** 直播内容 / 分区名，例如「原神」「王者荣耀」。 */
    val areaName: String,
    /** 接口给的「x.x万人看过」原文；为空时界面按 online 兜底。 */
    val watchedText: String = "",
)

/** 直播首页一屏：关注直播 + 分区入口 + 推荐卡片（对应 PiliPlus 的 card_list）。 */
data class LiveHome(
    val following: List<LiveCard> = emptyList(),
    val areas: List<LiveArea> = emptyList(),
    val cards: List<LiveCard> = emptyList(),
)

/** 直播分区（第二行横向列表）。parentId 为 0 表示这是一级分区本身。 */
data class LiveArea(
    val id: Long,
    val name: String,
    val parentId: Long = 0L,
)
