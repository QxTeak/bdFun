package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch

@Composable
internal fun FeedRefreshEffect(
    state: FeedState, loggedIn: Boolean, refreshTick: Int,
    request: FeedRefreshRequestStateHolder, pull: FeedPullSession,
) {
    LaunchedEffect(state, loggedIn, refreshTick) {
        if (refreshTick == request.handledTick) {
            state.ensureLoaded(loggedIn)
            return@LaunchedEffect
        }
        request.handledTick = refreshTick
        if (state.refreshing || state.loading) return@LaunchedEffect
        // 和首页再次点击底栏完全同一套：先回顶（不在顶部时和下拉动画并行），
        // 再把下拉指示器拉到阈值，最后刷新。
        val atTop = state.listState.firstVisibleItemIndex == 0 &&
            state.listState.firstVisibleItemScrollOffset == 0
        if (atTop) {
            pull.animatePullTo(pull.thresholdPx, 200f)
        } else {
            kotlinx.coroutines.coroutineScope {
                launch { state.listState.animateScrollToItem(0) }
                launch { pull.animatePullTo(pull.thresholdPx, 260f) }
            }
        }
        state.pullFraction = 1f
        state.refresh(loggedIn)
    }

}
