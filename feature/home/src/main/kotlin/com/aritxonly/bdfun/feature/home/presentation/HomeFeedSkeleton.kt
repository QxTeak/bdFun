package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import com.aritxonly.teak.common.chrome.TeakLoadingArea

/**
 * 首页/推荐首屏加载：Miuix 加载动画放在顶栏以下内容区域的正中。
 */
internal fun LazyGridScope.homeFeedSkeletonItems() {
    item(key = "home-loading", span = { GridItemSpan(maxLineSpan) }) {
        TeakLoadingArea()
    }
}
