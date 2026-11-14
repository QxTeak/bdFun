package com.aritxonly.bdfun.core.model.live

/** 直播页卡片：关注列表与分区列表共用。 */
data class LiveCard(
    val roomId: Long,
    val title: String,
    val uname: String,
    val cover: String,
    /** 直播内容 / 分区名，例如「原神」「王者荣耀」。 */
    val areaName: String,
    /** 接口给的「x人看过」原文（watched_show.text_large）；为空时界面按 online 兜底。 */
    val watchedText: String = "",
)

/** 直播首页一屏：关注直播 + 分区入口 + 推荐卡片（对应 PiliPlus 的 card_list）。 */
data class LiveHome(
    val following: List<LiveFollower> = emptyList(),
    val areas: List<LiveArea> = emptyList(),
    val cards: List<LiveCard> = emptyList(),
    /** 接口的 has_more；1 表示还能继续翻页。 */
    val hasMore: Boolean = false,
)

/**
 * 「我的关注正在直播」的 UP 主。
 * 和动态页顶部头像行同一个数据源（动态 portal 的 live_users），两边头像/文案才一致。
 */
data class LiveFollower(
    val roomId: Long,
    val mid: Long,
    val name: String,
    val face: String,
    val title: String = "",
)

/**
 * 直播分区（第二行横向列表）。
 *
 * `area_entrance_v3` 的每一项同时带 `area_v2_id` 和 `area_v2_parent_id`，
 * 而且「虚拟主播」这类入口的 area_v2_id 就是 0——所以两个请求参数必须分开存，
 * 不能像以前那样合成一个 id，否则点进去会带错分区。
 */
data class LiveArea(
    /** 选中态与列表 key（area_v2_id 为 0 时退回 parent id，保证唯一）。 */
    val id: Long,
    val name: String,
    /** 请求用的 parent_area_id。 */
    val parentAreaId: Long = 0L,
    /** 请求用的 area_id，允许为 0。 */
    val areaId: Long = 0L,
)
