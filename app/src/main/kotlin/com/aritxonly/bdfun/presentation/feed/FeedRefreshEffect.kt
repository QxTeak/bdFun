package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
internal fun FeedRefreshEffect(
    state: FeedState, loggedIn: Boolean, refreshTick: Int, pull: com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession,
) {
    // 首次进入 / 登录态变化只补一次加载；刷新请求由下面共用的编排处理。
    LaunchedEffect(state, loggedIn) { state.ensureLoaded(loggedIn) }
    // 和首页再次点击底栏完全同一套：先回顶（不在顶部时和下拉动画并行），
    // 再把下拉指示器拉到阈值，最后刷新。动态页是普通 LazyListState，所以几何用回调用。
    com.aritxonly.bdfun.feature.home.presentation.RefreshToTopEffect(
        request = refreshTick,
        pullThresholdPx = pull.thresholdPx,
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
        isBusy = { state.refreshing || state.loading },
        onPulling = { state.pullFraction = it },
        isAtTop = {
            state.listState.firstVisibleItemIndex == 0 &&
                state.listState.firstVisibleItemScrollOffset == 0
        },
        scrollToTop = { state.listState.animateScrollToItem(0) },
        refresh = { state.refresh(loggedIn) },
    )
}
