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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.pgc.application.BangumiRankSection
import com.aritxonly.bdfun.presentation.pgc.BangumiPageStateHolder
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
internal fun BangumiPage(
    state: BangumiPageStateHolder,
    topBarHeight: androidx.compose.ui.unit.Dp = HomeTopBarHeight,
) {
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
    // 不在页面里自动 scrollToItem(0)：网格状态是单例，保留离开时的位置；
    // 首屏/榜单/追番晚到都不再抢用户的滚动位置。
    // 顶部头图：优先当周新番；不足 10 张时用榜单/推荐补位，保证扇形能循环与左右滑动。
    val bannerItems = remember(rankUi.sections, state.cards, state.timeline) {
        bangumiBannerItems(state, rankUi.sections)
    }
    LaunchedEffect(state.timeline) { state.ensureTimelineIntros() }
    BangumiPageEffects(state, pull, pullUi)

    val landscape = androidx.compose.ui.platform.LocalConfiguration.current.orientation ==
        android.content.res.Configuration.ORIENTATION_LANDSCAPE
    // 横屏右侧不留 padding，左侧保留 10dp；竖屏左右都保留 10dp。
    val startPadding = 10.dp
    val endPadding = if (landscape) 0.dp else 10.dp
    val columns = remember(landscape) {
        if (landscape) {
            GridCells.Adaptive(120.dp)
        } else {
            GridCells.Fixed(3)
        }
    }
    LazyVerticalGrid(
        // 横屏用 Adaptive：卡片约 120dp，并把剩余宽度摊到每一列，整行从屏幕最左铺到最右，
        // 不会因为固定 120dp 上限而在左右留下空白。
        columns = columns,
        state = gridState,
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, pullUi.offset.roundToInt()) }
            .nestedScroll(pull.connection()),
        contentPadding = PaddingValues(
            start = startPadding,
            end = endPadding,
            top = topBarHeight + statusBarTop + 6.dp,
            bottom = 104.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "pgc-banner", span = { GridItemSpan(maxLineSpan) }) {
            BangumiBanner(bannerItems) { seasonId -> openBangumi(context, seasonId, scope) }
        }
        bangumiFollowSection(state, context, startPadding)
        bangumiRankSections(
            sections = rankUi.sections,
            holder = rankState,
            onOpenSeason = { seasonId -> openBangumi(context, seasonId, scope) },
            onOpenBoard = { section ->
                section.tabs.firstOrNull()?.let { tab ->
                    com.aritxonly.bdfun.BangumiRankBoardActivity.open(context, tab, section.title)
                }
            },
            sidePadding = startPadding,
            endPadding = endPadding,
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

private fun bangumiBannerItems(
    state: BangumiPageStateHolder,
    sections: List<BangumiRankSection>,
): List<BangumiBannerItem> {
    val fromTimeline = state.timeline.mapNotNull { card ->
        if (card.cover.isBlank()) null
        else BangumiBannerItem(card.cover, card.title, card.subtitle, card.seasonId)
    }
    val fromRank = sections.firstOrNull()?.entries.orEmpty().mapNotNull { entry ->
        val image = entry.banner.ifBlank { entry.cover }
        if (image.isBlank()) null
        else BangumiBannerItem(image, entry.title, entry.subtitle, entry.seasonId)
    }
    val fromCards = state.cards.map {
        BangumiBannerItem(it.cover, it.title, it.indexShow, it.seasonId)
    }
    return (fromTimeline + fromRank + fromCards).distinctBy { it.seasonId }.take(10)
}

@Composable
private fun rememberBangumiPullSession(    state: BangumiPageStateHolder,
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
    // 与推荐/直播共用同一份「回顶 + 下拉 + 刷新」编排。
    com.aritxonly.bdfun.feature.home.presentation.HomeSectionRefreshEffect(
        request = state.refreshRequest,
        gridState = state.gridState,
        pullThresholdPx = pull.thresholdPx,
        animatePull = { target, duration -> pull.animatePullTo(target, duration) },
        isBusy = { state.refreshing || state.loading },
        onPulling = { state.pullFraction = it },
        refresh = { state.refresh() },
    )
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
