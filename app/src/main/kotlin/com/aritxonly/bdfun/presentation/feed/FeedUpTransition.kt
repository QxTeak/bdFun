package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import com.aritxonly.teak.common.search.teakSearchUnderlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * 进入 UP 页的一套视觉参数与动作。
 *
 * 单独打包：主页面为它保留的组合期状态太多，直接铺在参数里会超过尺寸门槛，
 * 也让「转场」这件事只在一个地方解释。
 */
internal class FeedUpVisual(
    val upFocus: Boolean,
    val upPageVisible: Boolean,
    /** 当前分类（all / video / pgc）：UP 页的卡片跟着它过滤。 */
    val tabKey: String,
    /** 0 → 1 的弹簧进度（搜索一镜到底同一条时间线）。 */
    val progress: Float,
    /** 底层退让进度：没有目标页时为 0，退场时继续用 progress 反向播。 */
    val underlayProgress: Float,
    /** UP 页从卡片原位升到顶栏的初始下移距离。 */
    val risePx: Float,
    /** 退让层要画的「切页前那一份列表」及其滚动位置。 */
    val underlayItems: List<FeedItem>,
    val underlayList: LazyStaggeredGridState,
    /** 动态页那张头像卡片用的环：固定停在「全部动态」，和 UP 页的动环是两份状态。 */
    val cardRing: FeedAvatarRingState,
    /** 顶部下拉退出手势：跟手倒放同一套一镜到底。 */
    val pull: FeedUpPullController,
    val enterUp: (Long) -> Unit,
    val exitUp: () -> Unit,
)

/**
 * UP 页顶部下拉退出的手势状态。
 *
 * 进度只有一条：1 = UP 页完整在场，0 = 完全回到动态页。
 * 拖动时直接按位移写进度，松手再决定补完退场还是弹回；
 * 所以慢慢拉是逐帧倒放，快速往下甩则直接播完同一条退场动画。
 */
internal class FeedUpPullController(
    private val scope: CoroutineScope,
    private val progress: () -> Float,
    private val setProgress: (Float) -> Unit,
    private val distancePx: Float,
    private val flingVelocityPx: Float,
    private val spec: AnimationSpec<Float>,
    private val onComplete: suspend () -> Unit,
) {
    var pressed by mutableStateOf(false)
        private set
    private var dragPx = 0f
    private var job: Job? = null

    fun start() {
        job?.cancel()
        pressed = true
        // 支持从动画中途接着拖，不从 0 重新起算。
        dragPx = (1f - progress()) * distancePx
    }

    fun drag(deltaPx: Float) {
        dragPx = (dragPx + deltaPx).coerceIn(0f, distancePx)
        setProgress(feedUpExitProgress(dragPx, distancePx))
    }

    fun end(velocityPx: Float) {
        pressed = false
        if (shouldCompleteFeedUpExit(progress(), velocityPx, flingVelocityPx)) {
            job?.cancel()
            job = scope.launch {
                animate(progress(), 0f, animationSpec = spec) { value, _ -> setProgress(value) }
                onComplete()
            }
        } else {
            animateTo(1f)
        }
    }

    fun cancel() {
        pressed = false
        animateTo(if (progress() > 0f) 1f else 0f)
    }

    /** 供外部（返回键 / 底栏返回）把动画播到目标；不会触发 [onComplete]。 */
    fun animateTo(target: Float) {
        job?.cancel()
        job = scope.launch {
            animate(progress(), target, animationSpec = spec) { value, _ -> setProgress(value) }
        }
    }
}

/**
 * UP 页的进出状态：点头像 → 上移开新页；点「全部动态」/返回键 → 原路退回。
 *
 * 之所以把底层列表冻结一份：`filterTo` 会立刻把 `state.items` 换成目标 UP 的内容，
 * 直接复用会出现「卡片先消失、空白再上移」的闪帧；滚动位置同理（`enterUp` 会把
 * `state.listState` 归零，退让层要停在用户原来看到的位置）。
 */
@Composable
internal fun rememberFeedUpTransition(
    state: FeedState,
    tabKey: String,
    loggedIn: Boolean,
    scope: CoroutineScope,
    /** 同一份一镜到底进度上报给主壳：顶部分段与内容必须共用一条时间线。 */
    onProgress: (Float) -> Unit = {},
): FeedUpVisual {
    val upFocus = state.filterMid != null
    val upOrder = feedUpMids(state, AppGraph.session.value?.mid)
    val upPageVisible = upFocus && upOrder.isNotEmpty()
    val control = rememberFeedUpControl(state, tabKey, loggedIn, scope, upFocus, upPageVisible, onProgress)
    // 卡片顶上边距是「顶栏高 + 8dp」、内部还有 14dp，新顶栏只有 4dp：
    // 把 UP 页整块下移这个差值起步，头像看起来就是从列表里那张卡片升上来的。
    val risePx = with(LocalDensity.current) { (FeedTopBarHeight + 16.dp).toPx() }
    // 转场期间新旧两行头像同时在场：卡片静态环和 UP 页的动环必须是两份状态。
    val cardRing = remember { FeedAvatarRingState() }
    // 进入且确实有 UP 页时才推退让进度；没有目标页（作者列表还没到）就保持原列表。
    val underlayProgress = if (upPageVisible || !upFocus) control.progress else 0f
    return FeedUpVisual(
        upFocus = upFocus,
        upPageVisible = upPageVisible,
        tabKey = tabKey,
        progress = control.progress,
        underlayProgress = underlayProgress,
        risePx = risePx,
        underlayItems = control.underlayItems,
        underlayList = control.underlayList,
        cardRing = cardRing,
        pull = control.pull,
        enterUp = control.enterUp,
        exitUp = control.exitUp,
    )
}

private class FeedUpControl(
    val progress: Float,
    val pull: FeedUpPullController,
    val underlayItems: List<FeedItem>,
    val underlayList: LazyStaggeredGridState,
    val enterUp: (Long) -> Unit,
    val exitUp: () -> Unit,
)

/** 退让层、进入/退出动作和下拉跟手控制器；主转场函数只负责几何与分层。 */
@Composable
private fun rememberFeedUpControl(
    state: FeedState,
    tabKey: String,
    loggedIn: Boolean,
    scope: CoroutineScope,
    upFocus: Boolean,
    upPageVisible: Boolean,
    onProgress: (Float) -> Unit,
): FeedUpControl {
    var underlayItems by remember { mutableStateOf(state.items) }
    var underlayIndex by remember { mutableIntStateOf(0) }
    var underlayOffset by remember { mutableIntStateOf(0) }
    LaunchedEffect(state.items, upFocus) {
        if (!upFocus) underlayItems = state.items
    }
    // 从「全部动态」进入某个 UP：提交筛选后由 pager 直接组合出目标页。
    val enterUp: (Long) -> Unit = { mid ->
        scope.launch {
            underlayIndex = state.listState.firstVisibleItemIndex
            underlayOffset = state.listState.firstVisibleItemScrollOffset
            state.listState.scrollToItem(0)
            state.filterTo(mid)
        }
    }
    // 退出 UP 页：先把当前首屏留成预览，退场动画才有内容可退，再清筛选。
    val exitUp: () -> Unit = {
        scope.launch {
            state.listState.scrollToItem(0)
            state.filterMid?.let { state.seedPreview(it, state.items) }
            state.clearFilter()
        }
    }
    // 返回键先退出 UP 模式，再退动态页；同样走一遍退场动画。
    BackHandler(enabled = upFocus) {
        scope.launch {
            state.filterMid?.let { state.seedPreview(it, state.items) }
            state.exitUpFocus(tabKey, loggedIn)
        }
    }
    // 和搜索一镜到底同一条弹簧（阻尼 0.97 / stiffness 247）。
    val spec: AnimationSpec<Float> = if (LocalSettingsBindings.current.animationsEnabled) {
        TeakSearchTransitionDefaults.spec()
    } else {
        tween(0)
    }
    var progress by remember { mutableFloatStateOf(if (upFocus) 1f else 0f) }
    // 主壳顶部 segment 不能自己跑一条补间：这里把内容正在用的进度原样上报。
    SideEffect { onProgress(progress) }
    val density = LocalDensity.current
    val distancePx = with(density) { FeedUpExitPullDistance.toPx() }
    val flingPx = with(density) { FeedUpExitFlingVelocity.toPx() }
    val pull = remember(scope, spec, state, tabKey, loggedIn) {
        FeedUpPullController(
            scope = scope,
            progress = { progress },
            setProgress = { progress = it },
            distancePx = distancePx,
            flingVelocityPx = flingPx,
            spec = spec,
            onComplete = {
                state.listState.scrollToItem(0)
                state.filterMid?.let { state.seedPreview(it, state.items) }
                // 回到当前分类，而不是一律切到「全部」。
                state.exitUpFocus(tabKey, loggedIn)
            },
        )
    }
    // 筛选变化（进入/返回键/底栏返回）时补间；手指按着拖拽条时让位给跟手进度。
    LaunchedEffect(upFocus, pull) {
        if (upFocus && pull.pressed) return@LaunchedEffect
        pull.animateTo(if (upFocus) 1f else 0f)
    }
    // 退让层自己的列表状态：UP 页同时在场时不能和它共用一个 LazyStaggeredGridState。
    val underlayList = remember { LazyStaggeredGridState() }
    // 进页那一帧把退让层滚到原列表的位置，否则它从顶部开始、和「上移」对不上。
    LaunchedEffect(upPageVisible) {
        if (upPageVisible) underlayList.scrollToItem(underlayIndex, underlayOffset)
    }
    return FeedUpControl(progress, pull, underlayItems, underlayList, enterUp, exitUp)
}

/**
 * 动态页的两层：底层分类列表（UP 页打开时退让）＋ UP 页本身。
 *
 * 底层用 [teakSearchUnderlay] 上移 106dp、模糊、淡出；UP 页整块从卡片原位升到顶栏，
 * 和搜索一镜到底的搜索页走同一条时间线。退场时同一条进度反向播。
 */
@Composable
internal fun FeedUpLayers(
    state: FeedState,
    statusTop: Dp,
    pullUi: FeedPullUi,
    actions: FeedActions,
    media: FeedMedia,
    ring: FeedAvatarRingState,
    up: FeedUpVisual,
    loggedIn: Boolean,
) {
    // alpha 一律是 1 - p：进场从 1 淡到 0，退场从 0 回到 1。
    if (!up.upPageVisible || up.underlayProgress < 0.999f) {
        Box(
            Modifier.fillMaxSize()
                // 进场动画期间底下的列表只是残影，不要接点击（它会露出点卡片/点头像）。
                .blockTouches(up.upPageVisible)
                .teakSearchUnderlay(
                    progress = up.underlayProgress,
                    alpha = 1f - up.underlayProgress,
                    blurProgress = up.underlayProgress,
                ),
        ) {
            FeedContent(
                state, statusTop, pullUi.pull, pullUi.connection, actions, media,
                up.cardRing,
                up.enterUp, up.exitUp,
                items = if (up.upPageVisible) up.underlayItems else state.items,
                listState = if (up.upPageVisible) up.underlayList else state.listState,
            )
        }
    }
    // 退场时（upFocus 已为假、进度还没归零）继续留着这一页把它淡出去。
    if (up.upPageVisible || (!up.upFocus && up.progress > 0.001f)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = up.progress
                translationY = (1f - up.progress) * up.risePx
            },
        ) {
            FeedUpPagerLayout(
                state = state,
                ring = ring,
                statusTop = statusTop,
                tabKey = up.tabKey,
                actions = actions,
                media = media,
                loggedIn = loggedIn,
                pull = up.pull,
                onExitUp = up.exitUp,
                // 退场中这一页只负责淡出，不能再改筛选/抢手势。
                interactive = up.upPageVisible,
            )
        }
    }
}
