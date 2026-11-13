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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.live.application.LivePageStateHolder
import com.aritxonly.bdfun.navigateToLiveRoom
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullSession
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullUi
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 首页第一段「直播」。
 * 与推荐页共用同一套外壳：TrackTeakPageScroll 驱动顶栏渐进模糊/玻璃，
 * 下拉刷新在 App 层画小电视，本页只负责内容与手势。
 */
@Composable
internal fun LivePage(state: LivePageStateHolder, loggedIn: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gridState = state.gridState
    val pullUi = remember { DiscoverPullUi() }
    val pull = rememberLivePullSession(state, pullUi, scope)
    LivePageEffects(state, pull, pullUi)

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, pullUi.offset.roundToInt()) }
            .nestedScroll(pull.connection()),
        contentPadding = PaddingValues(
            start = 12.dp,
            end = 12.dp,
            top = HomeTopBarHeight + statusBarTop + 6.dp,
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        liveFollowSection(state)
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
    scope: kotlinx.coroutines.CoroutineScope,
): DiscoverPullSession {
    val density = LocalDensity.current
    val threshold = with(density) { 88.dp.toPx() }
    return remember(state) {
        DiscoverPullSession(
            density = density,
            thresholdPx = threshold,
            maxPullPx = with(density) { 260.dp.toPx() },
            currentOffset = { ui.offset },
            onPull = {
                ui.offset = it
                state.pullFraction = (it / threshold).coerceIn(0f, 1f)
            },
            onPullDp = { state.pullOffsetDp = it },
            canScrollBackward = { !state.gridState.canScrollBackward },
            isRefreshing = { state.refreshing || state.loading },
            onRefresh = {
                // 与推荐页一致：松手立即把指示器顶到阈值，再开始刷新。
                state.pullFraction = 1f
                scope.launch { state.refresh() }
            },
        )
    }
}

@Composable
private fun LivePageEffects(state: LivePageStateHolder, pull: DiscoverPullSession, ui: DiscoverPullUi) {
    LaunchedEffect(state) { state.ensureLoaded() }
    // 刷新结束：先收小电视，再让内容平滑回顶（与推荐页同一条时序）。
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && ui.offset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            pull.animatePullTo(0f, 260f)
        }
    }
    // 滚到底部预取下一页
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                val total = state.gridState.layoutInfo.totalItemsCount
                if (state.cards.isNotEmpty() && total > 0 && last >= total - 2) state.loadMore()
            }
    }
}
