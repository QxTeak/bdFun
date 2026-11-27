package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.DynamicTabPolicy
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSession
import com.aritxonly.bdfun.feature.home.presentation.PullRefreshSettleEffect
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * 动态页：和首页完全同一套结构——顶栏胶囊 + HorizontalPager。
 *
 * 每个分类拥有独立 [FeedState]（列表/滚动/分页/下拉互不影响），
 * 手指左右滑动与顶栏点选共用同一个 pager 状态，选中态写回 shell 供顶栏使用。
 *
 * 选中某个 UP 后进入「UP 模式」：顶部 pager 锁住，横向滑动改为切换上一个/下一个 UP，
 * 返回键、点顶部分段、点「全部动态」都会退出。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    loggedIn: Boolean, onNeedLogin: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit, onOpenFeed: (FeedItem) -> Unit, refreshTick: Int = 0,
    initialTab: String = "all",
    tabClickTick: Int = 0,
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
    val currentKey = tabs.getOrNull(pager.currentPage)?.key ?: tabs.first().key
    val upFocus = AppGraph.dynamicFeedStateFor(currentKey).filterMid != null
    FeedPagerSync(pager, tabs, initialTab, tabClickTick, loggedIn, onTabChange, onTabPositionChange)
    HorizontalPager(
        state = pager,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 0,
        // UP 模式下横向滑动归头像栏（切上一个/下一个 UP），不再切顶部分类。
        userScrollEnabled = !upFocus,
        overscrollEffect = null,
    ) { page ->
        val tab = tabs.getOrNull(page) ?: return@HorizontalPager
        androidx.compose.runtime.key(tab.key) {
            FeedTabPage(
                tabKey = tab.key,
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

/** pager 与顶栏、shell 之间的双向同步；单独拆出来避免主函数超过尺寸门槛。 */
@Composable
private fun FeedPagerSync(
    pager: PagerState,
    tabs: List<com.aritxonly.bdfun.core.domain.DynamicFeedTab>,
    initialTab: String,
    tabClickTick: Int,
    loggedIn: Boolean,
    onTabChange: (String) -> Unit,
    onPositionChange: (Float) -> Unit,
) {
    // 顶栏点选：只改 shell 状态，这里统一负责动画过去。
    var programmatic by remember { mutableStateOf(false) }
    val latestTabs by rememberUpdatedState(tabs)
    LaunchedEffect(initialTab, tabs) {
        val target = latestTabs.indexOfFirst { it.key == initialTab }.coerceAtLeast(0)
        // 换分段前先退出上一段的 UP 模式，回来时是干净的列表。
        val leaving = latestTabs.getOrNull(pager.currentPage)?.key
        if (leaving != null && leaving != initialTab) {
            AppGraph.dynamicFeedStateFor(leaving).exitUpFocus(leaving, loggedIn)
        }
        // 切顶部分类时菜单挂在主壳上，不会跟着列表离开，先收掉。
        leaving?.let { AppGraph.dynamicFeedStateFor(it).dismissMenu() }
        if (pager.currentPage != target || pager.currentPageOffsetFraction != 0f) {
            programmatic = true
            try {
                pager.animateScrollToPage(
                    target,
                    animationSpec = com.aritxonly.teak.common.navigation.TeakPageSwitchSpec(),
                )
            } finally {
                programmatic = false
            }
        }
    }
    // 点顶部分段（包括已经选中的那一段）= 退出 UP 模式。
    var lastTabClick by remember { mutableIntStateOf(tabClickTick) }
    LaunchedEffect(tabClickTick) {
        if (tabClickTick == lastTabClick) return@LaunchedEffect
        lastTabClick = tabClickTick
        val key = latestTabs.getOrNull(pager.currentPage)?.key ?: return@LaunchedEffect
        AppGraph.dynamicFeedStateFor(key).exitUpFocus(key, loggedIn)
    }
    // 手指划到哪一页，就把分类写回 shell（顶栏、回到顶部、刷新入口都读它）。
    val latestOnTabChange by rememberUpdatedState(onTabChange)
    LaunchedEffect(pager, tabs) {
        snapshotFlow { pager.currentPage }.collect { page ->
            val tab = latestTabs.getOrNull(page) ?: return@collect
            if (!programmatic) latestOnTabChange(tab.key)
        }
    }
    val latestOnPositionChange by rememberUpdatedState(onPositionChange)
    LaunchedEffect(pager, tabs) {
        snapshotFlow {
            (pager.currentPage + pager.currentPageOffsetFraction)
                .coerceIn(0f, latestTabs.lastIndex.toFloat())
        }.collect { latestOnPositionChange(it) }
    }
}

/**
 * UP 模式下的横向滑动：左滑下一个 UP、右滑上一个 UP。
 * 拖动期间页面跟手，松手后走整页推动；没过阈值就回弹，不换页。
 * 顶部 pager 此时已关掉滚动，不会和它抢同一个手势。
 */
private fun Modifier.feedUpModeSwipe(
    enabled: Boolean,
    transition: FeedUpPageTransition,
    scope: CoroutineScope,
    onRelease: (Int) -> Unit,
): Modifier = if (!enabled) this else this.pointerInput(transition) {
    var accumulated = 0f
    val threshold = 56.dp.toPx()
    detectHorizontalDragGestures(
        onDragStart = {
            accumulated = 0f
            transition.beginDrag(scope)
        },
        onHorizontalDrag = { change, dragAmount ->
            accumulated += dragAmount
            change.consume()
            transition.dragBy(dragAmount)
        },
        onDragEnd = {
            onRelease(
                when {
                    accumulated <= -threshold -> 1
                    accumulated >= threshold -> -1
                    else -> 0
                },
            )
        },
        onDragCancel = { onRelease(0) },
    )
}

@Composable
private fun FeedTabPage(
    tabKey: String,
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
    // 省略号/长按共用一份页面级菜单锚点；宿主挂在主壳采样树外的 MainFeedChrome。
    val menuUi = remember(state) { FeedCardMenuUi() }
    val actions = rememberFeedActions(
        state, scope, context, loggedIn, menuUi, onOpenVideo, onOpenFeed,
    )
    // 环放在页面级持有：头像卡片是懒列表第一项，滚出屏幕会被回收；状态挂在卡片里会丢。
    val ring = remember { FeedAvatarRingState() }
    // UP 之间切换：整页推动（旧页快照滑出、新页从另一侧滑入），和空间页翻页同一个观感。
    val transition = remember(state) { FeedUpPageTransition() }
    val selfMid = AppGraph.session.value?.mid
    val selection = rememberFeedUpSelection(state, scope, transition, selfMid, tabKey, loggedIn)
    val upFocus = state.filterMid != null
    // 返回键先退出 UP 模式，再退动态页。
    BackHandler(enabled = upFocus) { selection.exitToTab() }
    Box(
        modifier = Modifier.fillMaxSize()
            .feedUpModeSwipe(upFocus, transition, scope, selection.onSwipeRelease),
    ) {
        FeedCardMenuAnchorHost(menuUi)
        if (!loggedIn) {
            EmptyHint(
                title = "动态",
                subtitle = "登录后可查看关注 UP 主的动态",
                action = "扫码登录",
                onAction = onNeedLogin,
            )
        } else {
            FeedUpPageTransitionHost(transition) {
                FeedContent(
                    state, statusTop, pull, connection, actions, ring,
                    selection.selectUp, selection.exitUp,
                )
            }
            FeedIndicators(state, statusTop)
        }
        FeedReportHost(state, scope, context)
    }
}

/** 长按锚点先组合一帧，再让形变面板从手指位置展开（和首页同一时序）。 */
@Composable
private fun FeedCardMenuAnchorHost(menuUi: FeedCardMenuUi) {
    if (menuUi.point != androidx.compose.ui.geometry.Offset.Zero) {
        com.aritxonly.teak.common.navigation.TeakMorphPointAnchor(
            menuUi.pointMenu,
            menuUi.point,
        )
    }
    LaunchedEffect(menuUi.pendingOpen) {
        if (menuUi.pendingOpen) {
            withFrameNanos { }
            withFrameNanos { }
            menuUi.pointMenu.openWithAnchorCopy(false)
            menuUi.pendingOpen = false
        }
    }
}

/** 举报弹窗：菜单收起后仍由页面持有目标，独立于菜单宿主。 */
@Composable
private fun FeedReportHost(state: FeedState, scope: CoroutineScope, context: android.content.Context) {
    val item = state.reportItem ?: return
    DynamicReportDialog(
        item = item,
        onDismiss = { state.reportItem = null },
        onSubmit = { reason ->
            scope.launch {
                when (val result = state.reportDynamic(item, reason)) {
                    is com.aritxonly.bdfun.core.common.Outcome.Success ->
                        context.showToast("举报已提交")
                    is com.aritxonly.bdfun.core.common.Outcome.Failure ->
                        context.showToast(result.error.messageOr("举报失败"))
                    null -> Unit
                }
                state.reportItem = null
            }
        },
    )
}
