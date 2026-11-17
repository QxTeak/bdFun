package com.aritxonly.bdfun.presentation.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.presentation.live.LivePageStateHolder
import com.aritxonly.bdfun.navigateToLiveRoom
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullUi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 首页第一段「直播」。
 * 与推荐页共用同一套外壳：TrackTeakPageScroll 驱动顶栏渐进模糊/玻璃，
 * 下拉刷新在 App 层画小电视，本页只负责内容与手势。
 */
@Composable
internal fun LivePage(
    state: LivePageStateHolder,
    loggedIn: Boolean,
    topBarHeight: androidx.compose.ui.unit.Dp = HomeTopBarHeight,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gridState = state.gridState
    val pullUi = remember { DiscoverPullUi() }
    val pull = rememberLivePullSession(state, pullUi)
    LivePageEffects(state, pull, pullUi)

    LazyVerticalGrid(
        columns = if (LocalConfiguration.current.screenWidthDp >= 600) {
            com.aritxonly.bdfun.ui.BoundedVideoGridCells(180.dp, 260.dp)
        } else {
            GridCells.Fixed(2)
        },
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, pullUi.offset.roundToInt()) }
            .nestedScroll(pull.connection()),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = topBarHeight + statusBarTop + 6.dp,
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        liveFollowSection(state) { navigateToLiveRoom(context, it) }
        liveAreaSection(state, scope)
        items(state.cards, key = { it.roomId }) { live ->
            LiveGridCard(live) { navigateToLiveRoom(context, live.roomId) }
        }
        livePageStateItems(state, scope, loggedIn)
    }
}

@Composable
private fun rememberLivePullSession(
    state: LivePageStateHolder,
    ui: DiscoverPullUi,
): com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession {
    val density = LocalDensity.current
    val threshold = with(density) { 88.dp.toPx() }
    return remember(state) {
        com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession(
            thresholdPx = threshold,
            maxPullPx = with(density) { 260.dp.toPx() },
            density = density,
            currentOffset = { ui.offset },
            onPull = { px, dp, fraction ->
                ui.offset = px
                state.pullOffsetDp = dp
                state.pullFraction = fraction
            },
            // 这里传的是「网格自己能不能往回滚」，内部已经写了取反，不要再取反一次。
            canScrollBackward = { state.gridState.canScrollBackward },
            isRefreshing = { state.refreshing || state.loading },
            onRefresh = {
                // 与推荐页一致：松手立即把指示器顶到阈值，再开始刷新。
                state.pullFraction = 1f
                state.refresh()
            },
            onCanceled = { state.pullFraction = 0f },
        )
    }
}

@Composable
private fun LivePageEffects(state: LivePageStateHolder, pull: com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession, ui: DiscoverPullUi) {
    LaunchedEffect(state) { state.ensureLoaded() }
    // 与推荐/番剧/动态共用同一份「回顶 + 下拉 + 刷新」编排。
    com.aritxonly.bdfun.feature.home.presentation.RefreshToTopEffect(
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
    // 刷新结束：先收小电视，再让内容平滑回顶（与推荐页同一条时序）。
    // 刷新结束：先收小电视，再让内容平滑回顶（四个页面共用同一条收尾时序）。
    com.aritxonly.bdfun.feature.home.presentation.PullRefreshSettleEffect(
        refreshing = state.refreshing,
        currentOffset = { ui.offset },
        onSettledStart = { state.pullFraction = 0f },
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
    )
    // 滚到底部预取下一页：与推荐页同一套（drop(1) 忽略首次布局，按卡片数比较）
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .drop(1)
            .collect { last ->
                if (state.cards.isNotEmpty() && last >= state.cards.size - 2) state.loadMore()
            }
    }
}
