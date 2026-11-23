package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakLoadingArea

/**
 * 首页/推荐首屏加载：Miuix 加载动画放在屏幕正中。
 *
 * 这个 item 本身已经处在列表的顶部 contentPadding 之后，如果再按「窗口减顶栏」
 * 去算高度，图标会整体再低一截；把顶部 padding 抵消掉，中心才真正落在屏幕中线。
 */
internal fun LazyGridScope.homeFeedSkeletonItems(extraTopInset: Dp = 0.dp) {
    item(key = "home-loading", span = { GridItemSpan(maxLineSpan) }) {
        TeakLoadingArea(extraTopInset = extraTopInset)
    }
}
