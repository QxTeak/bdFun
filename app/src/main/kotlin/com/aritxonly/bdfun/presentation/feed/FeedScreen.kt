package com.aritxonly.bdfun.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSettleEffect
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings

/**
 * 动态页：和首页完全同一套结构——顶栏胶囊 + HorizontalPager。
 *
 * 每个分类拥有独立 [FeedState]（列表/滚动/分页/下拉互不影响），
 * 手指左右滑动与顶栏点选共用同一个 pager 状态，选中态写回 shell 供顶栏使用。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenFeed: (FeedItem) -> Unit, refreshTick: Int = 0,
    initialTab: String = "all",
    onTabChange: (String) -> Unit = {},
    onTabPositionChange: (Float) -> Unit = {},
) {
    val settings = LocalSettingsBindings.current
    val tabs = remember(settings.dynamicFeedTabs, settings.dynamicFeedTabOrder) {
        DynamicTabPolicy.ordered(settings.dynamicFeedTabs, settings.dynamicFeedTabOrder)
    }
    if (tabs.isEmpty()) return
    val initialIndex = tabs.indexOfFirst { it.key == initialTab }.coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = initialIndex, pageCount = { tabs.size })
    // 顶栏点选：只改 shell 状态，这里统一负责动画过去。
    var programmatic by remember { mutableStateOf(false) }
    val latestTabs by rememberUpdatedState(tabs)
    LaunchedEffect(initialTab, tabs) {
        val target = latestTabs.indexOfFirst { it.key == initialTab }.coerceAtLeast(0)
        if (pager.currentPage != target || pager.currentPageOffsetFraction != 0f) {
            programmatic = true
            try {
                pager.animateScrollToPage(target)
            } finally {
                programmatic = false
            }
        }
    }
    // 手指划到哪一页，就把分类写回 shell（顶栏、回到顶部、刷新入口都读它）。
    val latestOnTabChange by rememberUpdatedState(onTabChange)
    LaunchedEffect(pager, tabs) {
        snapshotFlow { pager.currentPage }.collect { page ->
            val tab = latestTabs.getOrNull(page) ?: return@collect
            if (!programmatic) latestOnTabChange(tab.key)
        }
    }
    val latestOnPositionChange by rememberUpdatedState(onTabPositionChange)
    LaunchedEffect(pager, tabs) {
        snapshotFlow {
            (pager.currentPage + pager.currentPageOffsetFraction)
                .coerceIn(0f, latestTabs.lastIndex.toFloat())
        }.collect { latestOnPositionChange(it) }
    }
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        overscrollEffect = null,
    ) { page ->
        val tab = tabs.getOrNull(page) ?: return@HorizontalPager
        androidx.compose.runtime.key(tab.key) {
            FeedTabPage(
                state = AppGraph.dynamicFeedStateFor(tab.key),
                loggedIn = loggedIn,
                onNeedLogin = onNeedLogin,
                onOpenVideo = onOpenVideo,
                onOpenFeed = onOpenFeed,
                refreshTick = refreshTick,
            )
        }
    }
}

@Composable
private fun FeedTabPage(
    state: FeedState, loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenFeed: (FeedItem) -> Unit, refreshTick: Int,
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
