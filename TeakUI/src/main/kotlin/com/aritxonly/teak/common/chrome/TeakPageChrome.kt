package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * 页面标题折叠 / 返回键玻璃显隐的进度状态。
 *
 * 统一描述“内容滚到哪、大标题折叠到哪、返回键露出多少”，
 * 由页面在滚动时写入，由 scaffold / 顶栏 / 返回键读取。
 */
@Stable
class TeakPageScrollState {
    var scrollableState by mutableStateOf<androidx.compose.foundation.gestures.ScrollableState?>(null)
        internal set
    /** 是否已经滚出顶部，决定返回键是否露出玻璃背景。 */
    var backRevealed by mutableStateOf(false)
        private set

    /** 大标题折叠进度：0 = 还在内容里，1 = 已经收进顶栏。 */
    var collapse by mutableFloatStateOf(0f)
        private set

    /** 大标题相对静止位置已经上滑的距离（px）；回弹位移不计入，所以标题不会被带下去。 */
    var titleOffsetPx by mutableFloatStateOf(0f)
        private set

    var materialProgress by mutableFloatStateOf(0f)
        private set

    /**
     * 手指上滑收起、下滑重新出现的进度：0 = 完全显示，1 = 已收起。
     *
     * 和 [materialProgress] 的区别是它看的是「这一次滑动的方向」，不是滚了多远：
     * 只要往上滑就收起且保持收起，往下滑一点就重新露出，避免 tab 栏长期挡住内容。
     */
    var scrollAway by mutableFloatStateOf(0f)
        private set

    private var lastScrollPosition = Long.MIN_VALUE

    fun reset() {
        materialProgress = 0f
        backRevealed = false
        collapse = 0f
        titleOffsetPx = 0f
        scrollAway = 0f
        lastScrollPosition = Long.MIN_VALUE
    }

    /**
     * 根据滚动偏移更新状态。
     *
     * [offsetPx] 是列表第一项内部已滚动的距离；`firstVisibleItemIndex > 0` 视为完全折叠。
     */
    fun update(
        firstVisibleItemIndex: Int,
        offsetPx: Float,
        thresholdPx: Float = DefaultThresholdPx,
        collapseRangePx: Float = DefaultCollapseRangePx,
        density: Float = 1f,
    ) {
        val distance = if (firstVisibleItemIndex > 0) 28f else offsetPx / density
        materialProgress = resolveTeakMaterialProgress(distance)
        backRevealed = firstVisibleItemIndex > 0 || offsetPx > thresholdPx
        collapse = if (firstVisibleItemIndex > 0) {
            1f
        } else {
            (offsetPx / collapseRangePx).coerceIn(0f, 1f)
        }
        // 第一项已经滚出屏幕时，标题早就淡出；用一个大值把固定标题推出画面即可。
        titleOffsetPx = if (firstVisibleItemIndex > 0) 10_000f else offsetPx
        // 方向判定：往上滑（索引/偏移变大）整条收起，往下滑一点就重新露出来；回到顶部永远显示。
        val position = firstVisibleItemIndex.toLong() * 1_000_000L + offsetPx.toLong()
        if (firstVisibleItemIndex == 0 && offsetPx < 4f) {
            scrollAway = 0f
        } else if (lastScrollPosition != Long.MIN_VALUE) {
            val delta = position - lastScrollPosition
            if (delta > 6L) scrollAway = 1f else if (delta < -6L) scrollAway = 0f
        }
        lastScrollPosition = position
    }

    companion object {
        const val DefaultThresholdPx = 150f
        const val DefaultCollapseRangePx = 150f
    }
}

/**
 * 进程级默认的页面 chrome 状态。
 *
 * 单 Activity / App 层绘制返回键的工程可以直接用它；需要页面级隔离时，
 * 用 [rememberTeakPageScrollState] 创建独立实例并显式传给
 * [TeakPageScaffold] / [TeakTopBar] / [TeakLargeTitle]。
 */
object TeakPageChrome {
    val state = TeakPageScrollState()

    fun reset() = state.reset()
}

@Composable
fun rememberTeakPageScrollState(): TeakPageScrollState = remember { TeakPageScrollState() }

/** LazyColumn / LazyRow 版：页面滚动时更新 [TeakPageScrollState]。 */
@Composable
fun TrackTeakPageScroll(
    state: LazyListState,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    thresholdPx: Float = TeakPageScrollState.DefaultThresholdPx,
    collapseRangePx: Float = TeakPageScrollState.DefaultCollapseRangePx,
) {
    BindTeakPageScrollState(state, pageState)
    val density = LocalDensity.current.density
    LaunchedEffect(state, pageState, thresholdPx, collapseRangePx, density) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                pageState.update(index, offset.toFloat(), thresholdPx, collapseRangePx, density)
            }
    }
}

/** verticalScroll 版：个人主页等普通滚动容器用。 */
@Composable
fun TrackTeakPageScroll(
    state: androidx.compose.foundation.ScrollState,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    thresholdPx: Float = TeakPageScrollState.DefaultThresholdPx,
    collapseRangePx: Float = TeakPageScrollState.DefaultCollapseRangePx,
) {
    BindTeakPageScrollState(state, pageState)
    val density = LocalDensity.current.density
    LaunchedEffect(state, pageState, thresholdPx, collapseRangePx, density) {
        snapshotFlow { state.value }
            .collect { offset ->
                pageState.update(0, offset.toFloat(), thresholdPx, collapseRangePx, density)
            }
    }
}

/** LazyVerticalGrid 版：收藏 / 历史 / 稍后再看等卡片网格用。 */
@Composable
fun TrackTeakPageScroll(
    state: LazyGridState,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    thresholdPx: Float = TeakPageScrollState.DefaultThresholdPx,
    collapseRangePx: Float = TeakPageScrollState.DefaultCollapseRangePx,
) {
    BindTeakPageScrollState(state, pageState)
    val density = LocalDensity.current.density
    LaunchedEffect(state, pageState, thresholdPx, collapseRangePx, density) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                pageState.update(index, offset.toFloat(), thresholdPx, collapseRangePx, density)
            }
    }
}

/** Smoothstep from Deadliner's actual-position observer; never integrates gesture deltas. */
internal fun resolveTeakMaterialProgress(distanceDp: Float): Float {
    val p = ((distanceDp - 4f) / 24f).coerceIn(0f, 1f)
    return p * p * (3f - 2f * p)
}

val LocalTeakPageScrollState = androidx.compose.runtime.staticCompositionLocalOf { TeakPageChrome.state }

/** Masonry feeds use the same actual-position observer as lists and regular grids. */
@Composable
fun TrackTeakPageScroll(
    state: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
) {
    BindTeakPageScrollState(state, pageState)
    val density = LocalDensity.current.density
    LaunchedEffect(state, pageState, density) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }
            .collect { (index, offset) -> pageState.update(index, offset.toFloat(), density = density) }
    }
}

/** 只连接宽屏留白的滚动，不改变固定顶栏的材质或折叠进度。 */
@Composable
fun BindTeakPageScrollState(
    state: androidx.compose.foundation.gestures.ScrollableState,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
) {
    androidx.compose.runtime.DisposableEffect(state, pageState) {
        pageState.scrollableState = state
        onDispose {
            if (pageState.scrollableState === state) pageState.scrollableState = null
        }
    }
}
