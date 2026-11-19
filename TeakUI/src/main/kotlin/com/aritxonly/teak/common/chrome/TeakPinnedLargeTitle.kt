package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.utils.LocalOverScrollState

/**
 * 大标题在页面里的静止锚点：由 [TeakLargeTitle] 上报，由 [TeakPageScaffold] 画固定副本。
 *
 * [x] / [y] 已排除滚动位移，[width] 用于让副本按同样的宽度换行。
 */
internal data class TeakLargeTitleAnchor(
    val title: String,
    val subtitle: String?,
    val titleColor: Color,
    val subtitleColor: Color,
    val x: Float,
    val y: Float,
    val width: Float,
)

/**
 * 页面级大标题槽：列表里的 [TeakLargeTitle] 只占位并上报锚点，
 * 可见的那份由 [TeakPageScaffold] 画在滚动容器之外，所以顶部回弹不会带走标题。
 */
internal class TeakLargeTitleAnchorState {
    var present by mutableIntStateOf(0)
        private set
    var anchor by mutableStateOf<TeakLargeTitleAnchor?>(null)
        internal set
    var rootOrigin: Offset? = null

    fun register() { present++ }

    fun unregister() {
        present = (present - 1).coerceAtLeast(0)
        if (present == 0) anchor = null
    }
}

internal val LocalTeakLargeTitleAnchor = compositionLocalOf<TeakLargeTitleAnchorState?> { null }

/** 把列表里的大标题复制一份画到页面层，并按滚动偏移上移；没有锚点时不出现。 */
@Composable
internal fun TeakPinnedLargeTitle(
    state: TeakLargeTitleAnchorState,
    pageState: TeakPageScrollState,
) {
    val anchor = state.anchor ?: return
    val offsetPx = pageState.titleOffsetPx
    val width = with(LocalDensity.current) { anchor.width.toDp() }
    TeakLargeTitle(
        title = anchor.title,
        subtitle = anchor.subtitle,
        topSpacer = 0.dp,
        titleColor = anchor.titleColor,
        subtitleColor = anchor.subtitleColor,
        pageState = pageState,
        modifier = Modifier
            .offset { IntOffset(anchor.x.roundToInt(), (anchor.y - offsetPx).roundToInt()) }
            .width(width),
        drawnAsPinnedCopy = true,
    )
}

/**
 * [TeakLargeTitle] 内部调用：登记页面级大标题槽，并在布局后上报静止锚点。
 *
 * 只在没有回弹时上报：回弹是父级图层位移，不属于滚动偏移，上报会把标题一起带下去。
 */
@Composable
internal fun TeakLargeTitleAnchorEffect(
    state: TeakLargeTitleAnchorState?,
    title: String,
    subtitle: String?,
    titleColor: Color,
    subtitleColor: Color,
    pageState: TeakPageScrollState,
    modifier: Modifier,
): Modifier {
    if (state == null) return modifier
    DisposableEffect(state) {
        state.register()
        onDispose { state.unregister() }
    }
    val overscrolling = LocalOverScrollState.current.isOverScrollActive
    val scrollOffsetPx = pageState.titleOffsetPx
    return modifier.onGloballyPositioned { coordinates ->
        if (overscrolling) return@onGloballyPositioned
        val root = state.rootOrigin ?: return@onGloballyPositioned
        val bounds = coordinates.boundsInWindow()
        val x = bounds.left - root.x
        val y = bounds.top - root.y + scrollOffsetPx
        val width = bounds.width
        val current = state.anchor
        val unchanged = current != null &&
            current.title == title && current.subtitle == subtitle &&
            current.titleColor == titleColor && current.subtitleColor == subtitleColor &&
            abs(current.x - x) < 0.5f && abs(current.y - y) < 0.5f && abs(current.width - width) < 0.5f
        if (unchanged) return@onGloballyPositioned
        state.anchor = TeakLargeTitleAnchor(title, subtitle, titleColor, subtitleColor, x, y, width)
    }
}
