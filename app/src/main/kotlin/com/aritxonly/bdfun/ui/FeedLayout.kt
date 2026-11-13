package com.aritxonly.bdfun.ui

internal const val FeedCardMinWidth = 280
internal const val FeedCardMaxWidth = 560

internal fun dynamicSidebarWidth(viewportWidthDp: Float): Float =
    if (viewportWidthDp < 736f) 0f else (viewportWidthDp * 0.2f).coerceIn(148f, 196f)
