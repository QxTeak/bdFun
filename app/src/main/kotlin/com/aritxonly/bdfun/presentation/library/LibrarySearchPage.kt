package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.chrome.LocalTeakPageScrollState
import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial
import com.aritxonly.teak.common.chrome.rememberTeakPageScrollState
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 搜索栏高度：和首页「点搜索后」的顶栏同一份尺寸（见 [HomeTopBarHeight]）。 */
private val SearchBarHeight = HomeTopBarHeight

/**
 * 媒体库搜索页。
 *
 * 结构对齐首页搜索页：内容（搜索记录 / 结果）整页滚动，顶部是一条
 * [ProgressiveTopBarMaterial] 搜索栏——内容滚到栏下时出现同款渐进式模糊，
 * 不再用一块不透明色带把顶部硬切开。
 *
 * 结果列表直接复用媒体库默认页的视频行（[LibrarySearchResults]），
 * 所以搜索结果页看起来就是「本页原本的 UI + 搜索栏」。
 *
 * 进场和首页底部搜索同一套：整页 300ms 淡入，内容和搜索栏从下方 106dp 弹上来；
 * 退场反过来，底色先透明，底层页面同时收回去。
 */
@Composable
internal fun LibrarySearchPage(
    content: LibrarySearchContent, actions: LibrarySearchActions,
    windowAlpha: Float, maskAlpha: Float, contentAlpha: Float, rise: Float,
) {
    val controller = remember { LibrarySearchStateHolder() }
    val state by controller.state.collectAsState()
    val events = LibrarySearchEvents(controller, actions)
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val liftPx = with(density) { TeakSearchTransitionDefaults.ContentLift.toPx() }
    val pageAlpha = windowAlpha.coerceIn(0f, 1f)
    val chromeAlpha = contentAlpha.coerceIn(0f, 1f)
    val mask = maskAlpha.coerceIn(0f, 1f)
    val reveal = rise.coerceIn(0f, 1f)
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 搜索页自己的滚动/采样状态，不能写回页面外壳那一份（否则退出后顶栏进度会被归零）。
    val scrollState = rememberTeakPageScrollState()
    // 搜索栏要采样内容做渐进模糊，和首页搜索页同一套录制层。
    val backdrop = rememberLayerBackdrop {
        drawRect(colors.background)
        drawContent()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = pageAlpha },
    ) {
        // 背景单独一条 100ms 时间线：退场先透明，避免整页白色拖影。
        Box(Modifier.matchParentSize().background(colors.background.copy(alpha = mask)))
        // 和首页搜索层一致：整页先铺一层吞触摸的垫层，动画透明期间点击也不会漏到下面。
        Box(Modifier.matchParentSize().blockTouches(true))
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = chromeAlpha
                    translationY = (1f - reveal) * liftPx
                },
        ) {
            CompositionLocalProvider(
                LocalTeakBackdrop provides backdrop,
                LocalTeakPageScrollState provides scrollState,
            ) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    if (!state.committed) {
                        SearchLanding(
                            history = content.history,
                            hot = emptyList(),
                            discovery = emptyList(),
                            query = state.draft,
                            suggestions = emptyList(),
                            onPickKeyword = events::chooseHistory,
                            onClearHistory = events::clearHistory,
                            topContentPadding = SearchBarHeight,
                            flatHistory = true,
                            roomyHistory = true,
                        )
                    } else {
                        LibrarySearchResults(content, actions, topInset = statusTop + SearchBarHeight + 6.dp)
                    }
                }
                CompositionLocalProvider(LocalTopBarScrollProgress provides scrollState.materialProgress) {
                    ProgressiveTopBarMaterial(
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        // 顶栏自己带状态栏避让，和首页搜索态完全同一棵树。
                        LibrarySearchHeader(content.title, state, events, onBack = actions.onBack)
                    }
                }
            }
        }
    }
}
