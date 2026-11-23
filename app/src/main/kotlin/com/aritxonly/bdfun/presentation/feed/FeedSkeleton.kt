package com.aritxonly.bdfun.ui

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan

/** 动态首屏加载：统一使用 Miuix 加载动画。 */
internal fun LazyStaggeredGridScope.dynamicFeedSkeletonItems() {
    item(key = "feed-loading", span = StaggeredGridItemSpan.FullLine) {
        MiuixLoadingRow()
    }
}
