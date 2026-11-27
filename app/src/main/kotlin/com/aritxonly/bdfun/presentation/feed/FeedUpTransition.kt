package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    val enterUp: (Long) -> Unit,
    val exitUp: () -> Unit,
)

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
): FeedUpVisual {
    val upFocus = state.filterMid != null
    val upOrder = feedUpMids(state, AppGraph.session.value?.mid)
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
    val animationsEnabled = LocalSettingsBindings.current.animationsEnabled
    val progress by animateFloatAsState(
        targetValue = if (upFocus) 1f else 0f,
        animationSpec = if (animationsEnabled) TeakSearchTransitionDefaults.spec() else tween(0),
        label = "feedUpPage",
    )
    // 卡片顶上边距是「顶栏高 + 8dp」、内部还有 14dp，新顶栏只有 4dp：
    // 把 UP 页整块下移这个差值起步，头像看起来就是从列表里那张卡片升上来的。
    val risePx = with(LocalDensity.current) { (FeedTopBarHeight + 16.dp).toPx() }
    val upPageVisible = upFocus && upOrder.isNotEmpty()
    // 转场期间新旧两行头像同时在场：卡片静态环和 UP 页的动环必须是两份状态。
    val cardRing = remember { FeedAvatarRingState() }
    // 退让层自己的列表状态：UP 页同时在场时不能和它共用一个 LazyStaggeredGridState。
    val underlayList = remember { LazyStaggeredGridState() }
    // 进页那一帧把退让层滚到原列表的位置，否则它从顶部开始、和「上移」对不上。
    LaunchedEffect(upPageVisible) {
        if (upPageVisible) underlayList.scrollToItem(underlayIndex, underlayOffset)
    }
    // 进入且确实有 UP 页时才推退让进度；没有目标页（作者列表还没到）就保持原列表。
    val underlayProgress = if (upPageVisible || !upFocus) progress else 0f
    return FeedUpVisual(
        upFocus = upFocus,
        upPageVisible = upPageVisible,
        tabKey = tabKey,
        progress = progress,
        underlayProgress = underlayProgress,
        risePx = risePx,
        underlayItems = underlayItems,
        underlayList = underlayList,
        cardRing = cardRing,
        enterUp = enterUp,
        exitUp = exitUp,
    )
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
                onExitUp = up.exitUp,
                // 退场中这一页只负责淡出，不能再改筛选/抢手势。
                interactive = up.upPageVisible,
            )
        }
    }
}
