package com.aritxonly.bdfun.presentation.discover

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
import com.aritxonly.bdfun.feature.discover.application.BangumiPageStateHolder
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 首页第三段「番剧」。外壳与推荐页一致（顶栏渐进模糊 + 下滑玻璃 + App 层下拉小电视），
 * 内容为「最近追番」横排 + 竖长封面推荐网格。
 */
@Composable
internal fun BangumiPage(state: BangumiPageStateHolder) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val gridState = state.gridState
    val pullUi = remember { DiscoverPullUi() }
    val pull = rememberBangumiPullSession(state, pullUi, scope)
    BangumiPageEffects(state, pull, pullUi)

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
        bangumiFollowSection(state, context)
        items(state.cards, key = { it.seasonId }) { item ->
            BangumiCardItem(item) { openBangumi(context, item.seasonId) }
        }
        bangumiPageStateItems(state, scope)
    }
}

@Composable
private fun rememberBangumiPullSession(
    state: BangumiPageStateHolder,
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
            isRefreshing = { state.refreshing },
            onRefresh = { scope.launch { state.refresh() } },
        )
    }
}

@Composable
private fun BangumiPageEffects(state: BangumiPageStateHolder, pull: DiscoverPullSession, ui: DiscoverPullUi) {
    LaunchedEffect(state) { state.ensureLoaded() }
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && ui.offset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            pull.animatePullTo(0f, 260f)
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .collect { last ->
                val total = state.gridState.layoutInfo.totalItemsCount
                if (state.cards.isNotEmpty() && total > 0 && last >= total - 2) state.loadMore()
            }
    }
}
