package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.teak.common.chrome.teakProgressiveBarBlur
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.theme.TeakUiStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

private const val FeedUpWideBreakpoint = 736


/** UP 页里「内容 + pager」的一组依赖，打包后悬浮页与两栏页都不用铺十几个参数。 */
private class FeedUpFeedContext(
    val state: FeedState,
    val pager: PagerState,
    val order: List<Long>,
    val actions: FeedActions,
    val media: FeedMedia,
    val liveMids: Set<Long>,
    val columns: StaggeredGridCells,
    val tabKey: String,
    val loggedIn: Boolean,
)

/**
 * UP 模式的内容层：和空间页一样用真正的 [HorizontalPager] 翻页。
 *
 * 窄屏的头像行就是这一页的顶栏：悬浮在内容之上，背后是渐进模糊，卡片从下面穿过去。
 * 宽屏保持作者侧栏 + pager 的两栏结构。
 *
 * 相邻页保持组合（`beyondViewportPageCount = 1`）：滑到一半时下一 UP 的首屏
 * 已经就位。每个 UP 的首屏会预取到 [FeedState] 的预览缓存，真正停稳后才提交筛选。
 */
@Composable
internal fun FeedUpPagerLayout(
    state: FeedState,
    ring: FeedAvatarRingState,
    statusTop: Dp,
    /** 当前分类（all / video / pgc）：UP 页的卡片也要跟着分段过滤。 */
    tabKey: String,
    actions: FeedActions,
    media: FeedMedia,
    loggedIn: Boolean,
    onExitUp: () -> Unit,
    /**
     * 退场动画期间为 false：这一页还在淡出，但不能再改筛选 / 抢手势。
     * 否则 pager 停稳回调会把已经退出的 UP 又 filterTo 回来。
     */
    interactive: Boolean = true,
) {
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= FeedUpWideBreakpoint
    val order = feedUpMids(state, AppGraph.session.value?.mid)
    val scope = rememberCoroutineScope()
    val handle = rememberFeedUpPagerHandle(state, order, scope, interactive)
    // 环的选中项跟 pager 走，比 filterMid 早：一开始翻页环就动，数据等停稳再换。
    var ringMid by remember { mutableStateOf(state.filterMid) }
    LaunchedEffect(state.filterMid) { ringMid = state.filterMid }
    LaunchedEffect(handle.pager.currentPage, order, interactive) {
        if (!interactive) return@LaunchedEffect
        order.getOrNull(handle.pager.currentPage)?.let { ringMid = it }
    }
    val selectUp: (Long) -> Unit = { mid ->
        // 点头像：环立刻出发，pager 同时开始滑。
        ringMid = mid
        handle.selectUp(mid)
    }
    val liveMids = remember(state.liveUsers) {
        state.liveUsers.mapTo(HashSet()) { it.mid }
    }
    val columns = if (wide) {
        BoundedVideoStaggeredGridCells(FeedCardMinWidth.dp, FeedCardMaxWidth.dp)
    } else {
        StaggeredGridCells.Fixed(1)
    }
    // 退场时整页还在淡出：吃掉点击，避免用户点到正在消失的头像/卡片。
    val rootModifier = Modifier.fillMaxSize().then(
        if (interactive) Modifier else Modifier.blockTouches(true),
    )
    if (wide) {
        val contentPadding = PaddingValues(
            start = TeakUiStyle.design.contentInset(8.dp),
            top = statusTop + 8.dp,
            end = TeakUiStyle.design.contentInset(8.dp),
            bottom = 100.dp,
        )
        Row(rootModifier) {
            FeedAuthorSidebar(
                state,
                modifier = Modifier.width(dynamicSidebarWidth(configuration.screenWidthDp.toFloat()).dp),
                topPadding = statusTop + 8.dp,
                bottomPadding = 100.dp,
                onSelectUp = selectUp,
                onExitUp = onExitUp,
            )
            UpPager(
                pager = handle.pager, order = order, state = state, actions = actions, media = media,
                liveMids = liveMids, columns = columns, contentPadding = contentPadding,
                tabKey = tabKey, exitPull = null, loggedIn = loggedIn, modifier = Modifier.weight(1f),
            )
        }
    } else {
        FeedUpFloatingPage(
            ring = ring,
            feed = FeedUpFeedContext(
                state = state,
                pager = handle.pager,
                order = order,
                actions = actions,
                media = media,
                liveMids = liveMids,
                columns = columns,
                tabKey = tabKey,
                loggedIn = loggedIn,
            ),
            statusTop = statusTop,
            ringMid = ringMid,
            selectUp = selectUp,
            onExitUp = onExitUp,
            interactive = interactive,
            rootModifier = rootModifier,
        )
    }
}

/**
 * 窄屏 UP 页：全屏内容 + 悬浮头像顶栏。
 *
 * 顶栏背后是这一页自己的 backdrop 采样出的渐进模糊（和首页顶栏同一套材质参数），
 * 所以卡片会从模糊下面穿过去；顶栏高度用测量值决定列表的顶部留白，不写死。
 */
@Composable
private fun FeedUpFloatingPage(
    feed: FeedUpFeedContext,
    ring: FeedAvatarRingState,
    statusTop: Dp,
    ringMid: Long?,
    selectUp: (Long) -> Unit,
    onExitUp: () -> Unit,
    interactive: Boolean,
    rootModifier: Modifier,
) {
    val density = LocalDensity.current
    val thresholdPx = with(density) { FeedUpExitPullThreshold.toPx() }
    val exitCallback by androidx.compose.runtime.rememberUpdatedState(onExitUp)
    // 下拉退出：这一页不刷新（刷新入口只留在分类列表上）。
    val bounds = remember(thresholdPx) { FeedUpExitPull(thresholdPx) { exitCallback() } }
    LaunchedEffect(bounds, feed.state.listState, interactive) {
        if (!interactive) return@LaunchedEffect
        snapshotFlow { feed.state.listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) bounds.settle()
        }
    }
    var barHeightPx by remember { mutableIntStateOf(0) }
    val barContentTop = with(density) { barHeightPx.toDp() } + 8.dp
    // 颜色在组合期取好：backdrop 的绘制 lambda 不是 @Composable，不能在里面读主题。
    val pageBackground = MiuixTheme.colorScheme.background
    val backdrop = rememberLayerBackdrop {
        drawRect(pageBackground)
        drawContent()
    }
    Box(rootModifier) {
        // 内容层（被顶栏采样，卡片会从模糊下穿过）。
        Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
            UpPager(
                pager = feed.pager, order = feed.order, state = feed.state, actions = feed.actions,
                media = feed.media, liveMids = feed.liveMids, columns = feed.columns,
                contentPadding = PaddingValues(
                    start = TeakUiStyle.design.contentInset(8.dp),
                    top = barContentTop,
                    end = TeakUiStyle.design.contentInset(8.dp),
                    bottom = 100.dp,
                ),
                tabKey = feed.tabKey, exitPull = bounds, loggedIn = feed.loggedIn,
                modifier = Modifier.fillMaxSize(),
            )
        }
        // 悬浮顶栏：头像本身就是这一页的顶栏，没有圆角卡片，也没有分类分段。
        Box(
            Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .teakProgressiveBarBlur(backdropOverride = backdrop)
                .onSizeChanged { barHeightPx = it.height },
        ) {
            FeedAvatarRow(
                state = feed.state,
                ring = ring,
                selectedMid = ringMid,
                onSelectUp = selectUp,
                onExitUp = onExitUp,
                includeLive = false,
                modifier = Modifier.padding(
                    start = TeakUiStyle.design.contentInset(8.dp),
                    end = TeakUiStyle.design.contentInset(8.dp),
                    top = statusTop + 4.dp,
                    bottom = 4.dp,
                ),
                contentPadding = PaddingValues(horizontal = 2.dp, vertical = 6.dp),
                // 环进场直接套在当前 UP 上，之后切换（左右滑 / 点头像）才播环绕动画。
                snapRingOnEnter = true,
            )
        }
    }
}

private class FeedUpPagerHandle(val pager: PagerState, val selectUp: (Long) -> Unit)

/** pager 状态与「点选后滑过去」的动作；停稳后才把 filterMid 提交过去。 */
@Composable
private fun rememberFeedUpPagerHandle(
    state: FeedState,
    order: List<Long>,
    scope: CoroutineScope,
    interactive: Boolean,
): FeedUpPagerHandle {
    val pager = rememberPagerState(
        initialPage = order.indexOf(state.filterMid).coerceAtLeast(0),
        pageCount = { order.size.coerceAtLeast(1) },
    )
    // 头像栏点选 / 外部改选中项时让 pager 滑过去；正在拖的时候不要抢手势。
    LaunchedEffect(state.filterMid, order, interactive) {
        if (!interactive) return@LaunchedEffect
        val index = order.indexOf(state.filterMid)
        if (index >= 0 && pager.settledPage != index && !pager.isScrollInProgress) {
            pager.animateScrollToPage(index)
        }
    }
    LaunchedEffect(pager, order, interactive) {
        if (!interactive) return@LaunchedEffect
        snapshotFlow { pager.settledPage }.collect { page ->
            val mid = order.getOrNull(page) ?: return@collect
            if (mid != state.filterMid) {
                // 停稳前先把旧页首屏留作预览，往回滑时不用重新等网络。
                state.filterMid?.let { leaving -> state.seedPreview(leaving, state.items) }
                state.listState.scrollToItem(0)
                state.filterTo(mid)
            }
        }
    }
    val selectUp: (Long) -> Unit = { mid ->
        val index = order.indexOf(mid)
        if (index >= 0) {
            if (index == pager.settledPage) {
                // 再点当前 UP = 刷新。
                scope.launch { state.filterTo(mid) }
            } else {
                scope.launch { pager.animateScrollToPage(index) }
            }
        }
    }
    return FeedUpPagerHandle(pager, selectUp)
}

/** pager 本体：当前页用常驻列表状态，邻页用首屏预览 + 独立滚动状态。 */
@Composable
private fun UpPager(
    pager: PagerState,
    order: List<Long>,
    state: FeedState,
    actions: FeedActions,
    media: FeedMedia,
    liveMids: Set<Long>,
    columns: StaggeredGridCells,
    contentPadding: PaddingValues,
    tabKey: String,
    exitPull: FeedUpExitPull?,
    loggedIn: Boolean,
    modifier: Modifier,
) {
    HorizontalPager(
        state = pager,
        modifier = modifier.fillMaxSize(),
        // 相邻页也保持组合，滑过去时内容已经就位。
        beyondViewportPageCount = 1,
        overscrollEffect = null,
    ) { page ->
        val mid = order.getOrNull(page) ?: return@HorizontalPager
        key(mid) {
            val live = mid == state.filterMid
            val rawItems = if (live) state.items else state.previewItems(mid).orEmpty()
            // 分段是「视频」时这一页只留视频动态，是「番剧」时只留 PGC 卡片。
            val items = feedItemsForTab(tabKey, items = rawItems)
            val gridState = if (live) {
                state.listState
            } else {
                remember(mid) { LazyStaggeredGridState() }
            }
            if (!live) {
                LaunchedEffect(mid, loggedIn) { state.prefetchUp(mid, loggedIn) }
            }
            val loading = live && state.loading
            Box(Modifier.fillMaxSize()) {
                FeedCardsGrid(
                    state = state,
                    items = items,
                    actions = actions,
                    media = media,
                    liveMids = liveMids,
                    listState = gridState,
                    columns = columns,
                    contentPadding = contentPadding,
                    modifier = if (live && exitPull != null) {
                        Modifier
                            .fillMaxSize()
                            .offset { IntOffset(0, exitPull.offset.roundToInt()) }
                            .nestedScroll(exitPull.connection)
                    } else {
                        Modifier.fillMaxSize()
                    },
                    showSkeleton = rawItems.isEmpty() && (loading || !live),
                    showFooter = live,
                )
                // 有数据但被当前分段筛空（比如这个 UP 没发过视频动态）：给一句提示，不留白屏。
                val emptyText = tabEmptyText(tabKey)
                if (!loading && emptyText != null && items.isEmpty() && rawItems.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = emptyText,
                        color = MiuixTheme.colorScheme.onBackgroundVariant,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = contentPadding.calculateTopPadding() + 32.dp),
                    )
                }
            }
        }
    }
}
