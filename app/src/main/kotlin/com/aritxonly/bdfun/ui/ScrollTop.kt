package com.aritxonly.bdfun.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import com.aritxonly.teak.common.chrome.teakGlassGlyphColor
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.aritxonly.teak.common.material.TeakGlassIconButton
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 当前列表页注册的「回到顶部」动作；没有注册时不显示按钮。 */


/** 第一项之前还有 3 项左右时认为已经离开顶部，足够避免刚打开就闪出按钮。 */
private fun scrolledEnough(index: Int, offset: Int): Boolean =
    index >= 3 || (index == 2 && offset > 0)

@Composable
fun BoxScope.ScrollTopButton(settings: SettingsBindings, bottomObstruction: androidx.compose.ui.unit.Dp = 0.dp) {
    val ScrollTopBus = LocalPageChromeState.current

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
        Box(Modifier.size(40.dp)) {
            if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                // 经典样式顶栏按钮都只留图标；回到顶部是全站唯一的玻璃圆钮，这里补上底。
                Box(
                    Modifier.matchParentSize()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .teakClassicBarGlass(),
                )
                Box(
                    Modifier.matchParentSize()
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.55f)),
                )
            }
            TeakGlassIconButton(
                onClick = { action?.invoke() },
                modifier = Modifier.size(40.dp),
            ) {
                // 图标统一走 AppGlyph（ICON-16 预设），不再手绘箭头。
                AppGlyph(LucideIcons.ArrowUp, glyphColor, size = 18.dp)
            }
        }
    }
}

@Composable
fun RegisterScrollTop(state: LazyListState, page: String) {
    val ScrollTopBus = LocalPageChromeState.current

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
    val ScrollTopBus = LocalPageChromeState.current

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
    val ScrollTopBus = LocalPageChromeState.current

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
