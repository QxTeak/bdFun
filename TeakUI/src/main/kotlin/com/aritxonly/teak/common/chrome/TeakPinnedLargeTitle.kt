package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** 页面内容上第一个大标题的文案；位置由页面外壳的统一 chrome 决定，不跟着列表量。 */
internal data class TeakLargeTitleData(
    val title: String,
    val subtitle: String?,
    val titleColor: Color,
    val subtitleColor: Color,
)

/**
 * 页面级大标题槽：列表里的 [TeakLargeTitle] 只占位并把文案登记到这里，
 * 可见的那份由 [TeakPageScaffold] 按统一位置画在滚动容器之外。
 */
internal class TeakLargeTitleAnchorState {
    var present by mutableIntStateOf(0)
        private set
    /** 组合期直接写入的普通字段，保证同一帧里外壳就能读到文案。 */
    var data: TeakLargeTitleData? = null

    /**
     * 页面内容自己在局部做「一镜到底」时的退让参数（设置页搜索等）。
     *
     * 内容只把列表包在退让层里，吸顶大标题却画在顶栏前景，两者不在同一棵子树，
     * 所以由调用方显式登记一份，标题在绘制阶段按同一份位移 / 淡出 / 模糊跟随。
     */
    val searchUnderlayProgress = mutableFloatStateOf(0f)
    val searchUnderlayAlpha = mutableFloatStateOf(1f)
    val searchUnderlayBlur = mutableFloatStateOf(0f)

    fun register() { present++ }

    fun unregister() {
        present = (present - 1).coerceAtLeast(0)
        if (present == 0) data = null
    }
}

internal val LocalTeakLargeTitleAnchor = compositionLocalOf<TeakLargeTitleAnchorState?> { null }

/**
 * 页面内容自己只对**局部内容**做「一镜到底」时（如设置页搜索：底部搜索框要留在原地，
 * 只有列表退让），把当前退让参数登记给页面外壳，让吸顶大标题和内容走同一份位移 / 淡出 / 模糊。
 *
 * 走 [TeakPageScaffold] 的 `underlayProgress` 那一套的页面不需要调用它，
 * 外壳会直接用自己那份参数；两者同时存在时按叠加处理。
 */
@Composable
fun TeakLargeTitleSearchUnderlay(
    progress: Float,
    alpha: Float = 1f,
    blur: Float = 0f,
) {
    val anchor = LocalTeakLargeTitleAnchor.current ?: return
    SideEffect {
        anchor.searchUnderlayProgress.floatValue = progress.coerceIn(0f, 1f)
        anchor.searchUnderlayAlpha.floatValue = alpha.coerceIn(0f, 1f)
        anchor.searchUnderlayBlur.floatValue = blur.coerceIn(0f, 1f)
    }
}

/**
 * 固定大标题：位置只取页面统一 chrome 的（状态栏 + 内容顶距、统一左右内边距），
 * 滚动时按列表偏移上移。因为不读列表里的标题坐标，回弹、首帧、快速滚动都不会把标题带偏。
 */
@Composable
internal fun TeakPinnedLargeTitle(
    state: TeakLargeTitleAnchorState,
    pageState: TeakPageScrollState,
    topInset: Dp = TeakChromeDefaults.ContentTopPadding,
    horizontalInset: Dp = TeakChromeDefaults.ContentHorizontalPadding,
) {
    if (state.present <= 0) return
    val data = state.data ?: return
    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .offset { IntOffset(0, -currentScrollOffsetPx(pageState).roundToInt()) },
        ) {
            TeakLargeTitle(
                title = data.title,
                subtitle = data.subtitle,
                titleColor = data.titleColor,
                subtitleColor = data.subtitleColor,
                pageState = pageState,
                modifier = Modifier.padding(start = horizontalInset, end = horizontalInset, top = topInset),
                drawnAsPinnedCopy = true,
            )
        }
    }
}

/**
 * 直接、同步地读取当前列表的滚动偏移（px）。
 *
 * 不能用 `snapshotFlow` 异步回写的进度：快速滚动时会晚一帧，标题会和内容错位。
 */
internal fun currentScrollOffsetPx(pageState: TeakPageScrollState): Float =
    when (val scrollable = pageState.scrollableState) {
        is androidx.compose.foundation.lazy.LazyListState ->
            if (scrollable.firstVisibleItemIndex > 0) 10_000f else scrollable.firstVisibleItemScrollOffset.toFloat()
        is androidx.compose.foundation.lazy.grid.LazyGridState ->
            if (scrollable.firstVisibleItemIndex > 0) 10_000f else scrollable.firstVisibleItemScrollOffset.toFloat()
        is androidx.compose.foundation.ScrollState -> scrollable.value.toFloat()
        else -> pageState.titleOffsetPx
    }
