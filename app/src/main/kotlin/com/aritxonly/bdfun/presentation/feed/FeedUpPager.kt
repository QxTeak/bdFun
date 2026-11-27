package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.teak.common.theme.TeakUiStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val FeedUpWideBreakpoint = 736

/**
 * UP 模式的内容层：和空间页一样用真正的 [HorizontalPager] 翻页。
 *
 * 头像行是选页控件，放在 pager 外面固定住（宽屏则是固定的作者侧栏），
 * 所以左右滑时只有动态卡片在动，UP 栏不会跟着走。
 *
 * 相邻页保持组合（`beyondViewportPageCount = 1`）：滑到一半时下一 UP 的首屏
 * 已经就位。每个 UP 的首屏会预取到 [FeedState] 的预览缓存，真正停稳后才提交筛选。
 */
@Composable
internal fun FeedUpPagerLayout(
    state: FeedState,
    ring: FeedAvatarRingState,
    statusTop: Dp,
    pull: FeedPullStateHolder,
    connection: NestedScrollConnection,
    actions: FeedActions,
    media: FeedMedia,
    loggedIn: Boolean,
    onExitUp: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val wide = configuration.screenWidthDp >= FeedUpWideBreakpoint
    val order = feedUpMids(state, AppGraph.session.value?.mid)
    val scope = rememberCoroutineScope()
    val handle = rememberFeedUpPagerHandle(state, order, scope)
    // 环的选中项跟 pager 走，比 filterMid 早：一开始翻页环就动，数据等停稳再换。
    var ringMid by remember { mutableStateOf(state.filterMid) }
    LaunchedEffect(state.filterMid) { ringMid = state.filterMid }
    LaunchedEffect(handle.pager.currentPage, order) {
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
    val contentPadding = PaddingValues(
        start = TeakUiStyle.design.contentInset(8.dp),
        // 窄屏的头像行已经固定在 pager 上方，页内不用再让顶栏；宽屏没有固定行，仍按原来的顶部避让。
        top = if (wide) statusTop + FeedTopBarHeight + 8.dp else 8.dp,
        end = TeakUiStyle.design.contentInset(8.dp),
        bottom = 100.dp,
    )
    if (wide) {
        Row(Modifier.fillMaxSize()) {
            FeedAuthorSidebar(
                state,
                modifier = Modifier.width(dynamicSidebarWidth(configuration.screenWidthDp.toFloat()).dp),
                topPadding = statusTop + FeedTopBarHeight,
                bottomPadding = 100.dp,
                onSelectUp = selectUp,
                onExitUp = onExitUp,
            )
            UpPager(
                handle.pager, order, state, actions, media, liveMids, columns, contentPadding,
                pull, connection, loggedIn, Modifier.weight(1f),
            )
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            FeedAvatarCard(
                state = state,
                ring = ring,
                onSelectUp = selectUp,
                onExitUp = onExitUp,
                selectedMid = ringMid,
                modifier = Modifier.padding(
                    start = TeakUiStyle.design.contentInset(8.dp),
                    end = TeakUiStyle.design.contentInset(8.dp),
                    top = statusTop + FeedTopBarHeight + 8.dp,
                    bottom = 4.dp,
                ),
            )
            UpPager(
                handle.pager, order, state, actions, media, liveMids, columns, contentPadding,
                pull, connection, loggedIn, Modifier.weight(1f),
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
): FeedUpPagerHandle {
    val pager = rememberPagerState(
        initialPage = order.indexOf(state.filterMid).coerceAtLeast(0),
        pageCount = { order.size.coerceAtLeast(1) },
    )
    // 头像栏点选 / 外部改选中项时让 pager 滑过去；正在拖的时候不要抢手势。
    LaunchedEffect(state.filterMid, order) {
        val index = order.indexOf(state.filterMid)
        if (index >= 0 && pager.settledPage != index && !pager.isScrollInProgress) {
            pager.animateScrollToPage(index)
        }
    }
    LaunchedEffect(pager, order) {
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
    pull: FeedPullStateHolder,
    connection: NestedScrollConnection,
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
            val items = if (live) state.items else state.previewItems(mid).orEmpty()
            val gridState = if (live) {
                state.listState
            } else {
                remember(mid) { LazyStaggeredGridState() }
            }
            if (!live) {
                LaunchedEffect(mid, loggedIn) { state.prefetchUp(mid, loggedIn) }
            }
            FeedCardsGrid(
                state = state,
                items = items,
                actions = actions,
                media = media,
                liveMids = liveMids,
                listState = gridState,
                columns = columns,
                contentPadding = contentPadding,
                modifier = if (live) {
                    Modifier
                        .fillMaxSize()
                        .offset { IntOffset(0, pull.pull.roundToInt()) }
                        .nestedScroll(connection)
                } else {
                    Modifier.fillMaxSize()
                },
                showSkeleton = items.isEmpty() && (live && state.loading || !live),
                showFooter = live,
            )
        }
    }
}
