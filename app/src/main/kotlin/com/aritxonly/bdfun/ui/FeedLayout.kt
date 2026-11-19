package com.aritxonly.bdfun.ui

internal const val FeedCardMinWidth = 280
internal const val FeedCardMaxWidth = 560

/** 首页推荐 / 直播 / 热门 / 收藏等视频网格共用的卡片宽度：宽屏只加列，不把卡片拉长。 */
internal const val RecommendedCardMinWidth = 180
internal const val RecommendedCardMaxWidth = 260

internal fun dynamicSidebarWidth(viewportWidthDp: Float): Float =
    if (viewportWidthDp < 736f) 0f else (viewportWidthDp * 0.2f).coerceIn(148f, 196f)
