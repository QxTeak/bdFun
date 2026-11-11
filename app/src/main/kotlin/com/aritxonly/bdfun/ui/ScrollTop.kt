package com.aritxonly.bdfun.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.material.TeakGlassIconButton
import kotlinx.coroutines.launch

/** 当前列表页注册的「回到顶部」动作；没有注册时不显示按钮。 */
object ScrollTopBus {
    var action by mutableStateOf<(() -> Unit)?>(null)
    /** 注册动作的页面 key，用于设置里按页面开关。 */
    var page by mutableStateOf<String?>(null)
    /** 页面是否已经滑过一小段；在顶部时按钮淡出。 */
    var scrolled by mutableStateOf(false)
}

/** 第一项之前还有 3 项左右时认为已经离开顶部，足够避免刚打开就闪出按钮。 */
private fun scrolledEnough(index: Int, offset: Int): Boolean =
    index >= 3 || (index == 2 && offset > 0)

@Composable
fun BoxScope.ScrollTopButton(settings: SettingsStore, bottomObstruction: androidx.compose.ui.unit.Dp = 0.dp) {
    val action = ScrollTopBus.action
    val page = ScrollTopBus.page
    val enabled = settings.scrollTopEnabled &&
        page != null &&
        settings.isScrollTopPageEnabled(page)
    val shown = enabled && action != null && ScrollTopBus.scrolled
    val glyphColor = teakGlassGlyphColor()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val start = settings.scrollTopSide == 0
    val navigationBottom = rememberNavigationBarBottom()
    val bottomPadding = navigationBottom + maxOf(screenHeight * 0.10f + 16.dp, bottomObstruction + 20.dp)
    AnimatedVisibility(
        visible = shown,
        modifier = Modifier
            .align(if (start) Alignment.BottomStart else Alignment.BottomEnd)
            // Keep at least 20dp above the actual bottom controls, including short landscape windows.
            .padding(
                start = if (start) 12.dp else 0.dp,
                end = if (start) 0.dp else 12.dp,
                bottom = bottomPadding,
            ),
        enter = fadeIn(tween(180)),
        exit = fadeOut(tween(140)),
    ) {
        TeakGlassIconButton(
            onClick = { action?.invoke() },
            modifier = Modifier.size(40.dp),
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val w = size.width
                val h = size.height
                val stroke = 1.8.dp.toPx()
                val color: Color = glyphColor
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.82f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.22f, h * 0.46f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(w * 0.5f, h * 0.18f),
                    end = androidx.compose.ui.geometry.Offset(w * 0.78f, h * 0.46f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
fun RegisterScrollTop(state: LazyListState, page: String) {
    val scope = rememberCoroutineScope()
    val action: () -> Unit = {
        scope.launch { state.animateScrollToItem(0) }
    }
    DisposableEffect(state, page) {
        ScrollTopBus.action = action
        ScrollTopBus.page = page
        ScrollTopBus.scrolled = false
        onDispose {
            if (ScrollTopBus.action === action) {
                ScrollTopBus.action = null
                ScrollTopBus.page = null
                ScrollTopBus.scrolled = false
            }
        }
    }
    LaunchedEffect(state, page) {
        snapshotFlow {
            scrolledEnough(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }.collect { ScrollTopBus.scrolled = it }
    }
}

@Composable
fun RegisterScrollTop(state: LazyGridState, page: String) {
    val scope = rememberCoroutineScope()
    val action: () -> Unit = {
        scope.launch { state.animateScrollToItem(0) }
    }
    DisposableEffect(state, page) {
        ScrollTopBus.action = action
        ScrollTopBus.page = page
        ScrollTopBus.scrolled = false
        onDispose {
            if (ScrollTopBus.action === action) {
                ScrollTopBus.action = null
                ScrollTopBus.page = null
                ScrollTopBus.scrolled = false
            }
        }
    }
    LaunchedEffect(state, page) {
        snapshotFlow {
            scrolledEnough(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }.collect { ScrollTopBus.scrolled = it }
    }
}

@Composable
fun RegisterScrollTop(state: androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState, page: String) {
    val scope = rememberCoroutineScope()
    val action: () -> Unit = {
        scope.launch { state.animateScrollToItem(0) }
    }
    DisposableEffect(state, page) {
        ScrollTopBus.action = action
        ScrollTopBus.page = page
        ScrollTopBus.scrolled = false
        onDispose {
            if (ScrollTopBus.action === action) {
                ScrollTopBus.action = null
                ScrollTopBus.page = null
                ScrollTopBus.scrolled = false
            }
        }
    }
    LaunchedEffect(state, page) {
        snapshotFlow {
            scrolledEnough(state.firstVisibleItemIndex, state.firstVisibleItemScrollOffset)
        }.collect { ScrollTopBus.scrolled = it }
    }
}
