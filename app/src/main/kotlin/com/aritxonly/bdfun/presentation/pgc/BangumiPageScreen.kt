package com.aritxonly.bdfun.presentation.pgc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.aritxonly.bdfun.feature.pgc.application.BangumiPageStateHolder
import com.aritxonly.bdfun.presentation.common.discover.DiscoverSectionTitle
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullSession
import com.aritxonly.bdfun.presentation.common.discover.DiscoverPullUi
import kotlinx.coroutines.flow.drop
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
    // 排行榜内嵌在追番下面；数据在 AppContainer，页面只做投影。
    val rankState = com.aritxonly.bdfun.AppGraph.bangumiRankState
    val rankUi by rankState.state.collectAsState()
    val pull = rememberBangumiPullSession(state, pullUi, scope)
    LaunchedEffect(rankState) { rankState.ensureLoaded() }
    // 进页面回到顶部。网格状态是单例持久的，切走再回来会留着上次的位置；
    // 更关键的是「最近追番」比榜单晚返回，它插在榜单上面时 LazyGrid 会按锚点不动，
    // 于是看起来一进来就停在热门动画榜。所以数据落定（loading 结束）后再回顶一次。
    LaunchedEffect(state, state.loading) {
        if (!state.loading) gridState.scrollToItem(0)
    }
    LaunchedEffect(rankUi.sections.size) {
        if (rankUi.sections.isNotEmpty()) gridState.scrollToItem(0)
    }
    BangumiPageEffects(state, pull, pullUi)

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, pullUi.offset.roundToInt()) }
            .nestedScroll(pull.connection()),
        contentPadding = PaddingValues(
            start = 10.dp,
            end = 10.dp,
            top = HomeTopBarHeight + statusBarTop + 6.dp,
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        bangumiFollowSection(state, context)
        bangumiRankSections(
            sections = rankUi.sections,
            holder = rankState,
            onOpenSeason = { seasonId -> openBangumi(context, seasonId, scope) },
            onOpenBoard = { section ->
                section.tabs.firstOrNull()?.let { tab ->
                    com.aritxonly.bdfun.BangumiRankBoardActivity.open(context, tab, section.title)
                }
            },
        )
        if (state.cards.isNotEmpty()) {
            item(key = "pgc-recommend-title", span = { GridItemSpan(maxLineSpan) }) {
                BangumiSectionTitle("推荐")
            }
        }
        items(state.cards, key = { it.seasonId }) { item ->
            BangumiCardItem(item) { openBangumi(context, item.seasonId, scope) }
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
            // 与直播/推荐页一致：传原始 canScrollBackward，别在调用处再取反
            // （取反会变成「不在顶部也能下拉刷新」）。
            canScrollBackward = { state.gridState.canScrollBackward },
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
private fun BangumiPageEffects(state: BangumiPageStateHolder, pull: DiscoverPullSession, ui: DiscoverPullUi) {
    LaunchedEffect(state) { state.ensureLoaded() }
    LaunchedEffect(state.refreshing) {
        if (!state.refreshing && ui.offset > 0f) {
            state.pullFraction = 0f
            kotlinx.coroutines.delay(160)
            pull.animatePullTo(0f, 260f)
        }
    }
    // 与推荐页同一套：首次布局会重发一次当前索引（进入页面/从别的页回来），
    // 不能当成用户滑到底，所以 drop(1)；比较对象是卡片数而不是 totalItemsCount
    // （后者把小标题、榜单分节、加载/错误项也算进去，触发点会漂）。
    LaunchedEffect(state) {
        snapshotFlow { state.gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }
            .drop(1)
            .collect { last ->
                if (state.cards.isNotEmpty() && last >= state.cards.size - 2) state.loadMore()
            }
    }
}
