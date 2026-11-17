package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@Composable
internal fun HomeFeedEffects(state: HomeFeedState, ui: HomePresentationStateHolder, pull: PullRefreshSession) {
    LaunchedEffect(state) { state.ensureLoaded() }
    // 刷新结束 → 先把小电视收掉，再让内容平滑回顶（四个页面共用同一条收尾时序）。
    PullRefreshSettleEffect(
        refreshing = state.refreshing,
        currentOffset = { ui.pullOffset },
        onSettledStart = { state.pullFraction = 0f },
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
    )

    // 外部请求刷新（再次点击底栏当前页）：首页各段与动态页共用同一份编排。
    RefreshToTopEffect(
        request = state.refreshRequest,
        pullThresholdPx = pull.thresholdPx,
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
        isBusy = { state.refreshing || state.loading },
        onPulling = { state.pullFraction = it },
        isAtTop = {
            state.gridState.firstVisibleItemIndex == 0 &&
                state.gridState.firstVisibleItemScrollOffset == 0
        },
        scrollToTop = { state.gridState.animateScrollToItem(0) },
        refresh = { state.refresh() },
    )

    // 滚到接近底部就预取下一页
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            // 进入页面/从视频页返回时首次布局会重发一次当前索引，不能把它当成用户滑到底。
            .drop(1)
            .collect { last ->
                if (state.videos.isNotEmpty() && last >= state.videos.size - (if (state.optimizationEnabled)
                    maxOf(2, state.gridState.layoutInfo.visibleItemsInfo.size * 2) else 2)) state.loadMore()
            }
    }

}
