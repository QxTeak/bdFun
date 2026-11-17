package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSettleEffect

@Composable
fun FeedScreen(
    state: FeedState, loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenFeed: (FeedItem) -> Unit, refreshTick: Int = 0,
) {
    val scope = rememberFeedTaskScope(state)
    val context = LocalContext.current
    val density = LocalDensity.current
    val pull = remember(state) { FeedPullStateHolder() }
    // 下拉刷新与首页三段共用同一个会话实现（阈值/阻尼/缓动完全一致）。
    val session = PullRefreshSession(
        thresholdPx = with(density) { FeedPullThreshold.toPx() },
        maxPullPx = with(density) { FeedPullMax.toPx() },
        density = density,
        currentOffset = { pull.pull },
        onPull = { px, dp, fraction ->
            pull.pull = px
            state.pullOffsetDp = dp
            state.pullFraction = fraction
        },
        canScrollBackward = { state.listState.canScrollBackward },
        isRefreshing = { state.refreshing },
        onRefresh = {
            state.pullFraction = 1f
            state.refresh(loggedIn)
        },
        onCanceled = { state.pullFraction = 0f },
    )
    PullRefreshSettleEffect(
        refreshing = state.refreshing,
        currentOffset = { pull.pull },
        onSettledStart = { state.pullFraction = 0f },
        animatePull = { target, duration -> session.animatePullTo(target, duration) },
    )
    val emotes = LocalRichTextDependencies.current.emotes
    LaunchedEffect(Unit) { emotes.ensure() }
    FeedRefreshEffect(state, loggedIn, refreshTick, session)
    val statusTop = rememberStatusBarTop()
    val connection = remember(state) { session.connection() }
    FeedPaginationEffects(state, loggedIn)
    val actions = FeedActions(state, scope, context, loggedIn, onOpenVideo, onOpenFeed)
    Box(modifier = Modifier.fillMaxSize()) {
        if (!loggedIn) {
            EmptyHint(
                title = "动态",
                subtitle = "登录后可查看关注 UP 主的动态",
                action = "扫码登录",
                onAction = onNeedLogin,
            )
        } else {
            FeedContent(state, statusTop, pull, connection, actions)
            FeedIndicators(state, statusTop)
        }
    }
}
