package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import kotlinx.coroutines.launch

/**
 * 点顶栏回顶：全站常开，不提供开关。
 *
 * 只铺在状态栏 + 顶栏这一条上，并且**画在顶栏按钮下面**：返回键 / 搜索框 / 分段
 * 仍然优先拿到点击，只有点到顶栏空白处（或状态栏没被系统吃掉的那一段）才回顶。
 * 页面没滚动（`canScrollBackward == false`）时点它也没有副作用。
 */
@Composable
fun TeakStatusBarScrollToTop(
    pageState: TeakPageScrollState,
    modifier: Modifier = Modifier,
    belowStatusBar: Dp = 0.dp,
) {
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    if (statusBar <= 0.dp) return
    val scope = rememberCoroutineScope()
    val interaction = remember { MutableInteractionSource() }
    val action = remember(pageState, scope) {
        {
            val scrollable = pageState.scrollableState
            if (scrollable != null && scrollable.canScrollBackward) {
                scope.launch { scrollTeakPageToTop(scrollable) }
            }
        }
    }
    // 状态栏点击不进应用窗口；把最近组合的页面回顶动作登记出去，供系统回顶回调复用。
    DisposableEffect(action) {
        TeakStatusBarTapBridge.register(action)
        onDispose { TeakStatusBarTapBridge.unregister(action) }
    }
    Box(
        modifier
            .fillMaxWidth()
            .height(statusBar + belowStatusBar.coerceAtLeast(0.dp))
            .semantics { contentDescription = "回到顶部" }
            .clickable(interactionSource = interaction, indication = null, onClick = action),
    )
}

/**
 * 当前页面的状态栏回顶动作；由 [TeakStatusBarScrollToTop] 登记，系统回顶回调通过它复用。
 *
 * 用栈而不是单个 handler：页面壳（主壳 / 二级页）可能同时存在，最近组合的那一份优先；
 * 它退出时只移除自己，主壳的动作仍然保留，不会把回顶能力一并清掉。
 */
object TeakStatusBarTapBridge {
    private val handlers = java.util.concurrent.CopyOnWriteArrayList<() -> Unit>()

    fun register(action: () -> Unit) {
        handlers.remove(action)
        handlers.add(action)
    }

    fun unregister(action: () -> Unit) {
        handlers.remove(action)
    }

    fun request(): Boolean {
        val action = handlers.lastOrNull() ?: return false
        action()
        return true
    }
}

/** 把页面当前的滚动容器收回顶部；只处理列表 / 网格 / 普通 ScrollState。 */
internal suspend fun scrollTeakPageToTop(state: ScrollableState) {
    when (state) {
        is LazyListState -> state.animateScrollToItem(0)
        is LazyGridState -> state.animateScrollToItem(0)
        is LazyStaggeredGridState -> state.animateScrollToItem(0)
        is ScrollState -> state.animateScrollTo(0)
    }
}
