package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.appMaterialColors
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.hyperDarkColors
import com.aritxonly.bdfun.hyperLightColors
import com.aritxonly.teak.common.chrome.TeakPageChrome
import com.aritxonly.teak.common.chrome.TeakPageScaffold
import com.aritxonly.teak.common.chrome.TeakPageScrollState
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakTheme

/**
 * 独立 Activity 的统一外壳：主题 + 玻璃页面 chrome（顶栏 / 返回键 / backdrop）。
 *
 * 页面 chrome 本体已抽到 `TeakUI` 的 [TeakPageScaffold]，
 * 这里只负责把 App 的主题、色板和字号注入进去。
 *
 * [backRevealed] 传 null 表示跟随页面滚动；视频页传 false（纯箭头）。
 */
@Composable
internal fun TeakActivityScaffold(
    settings: SettingsBindings, onBack: () -> Unit,
    options: ActivityPageOptions = ActivityPageOptions(),
    disc: ActivityDiscOptions = ActivityDiscOptions(),
    slots: ActivityPageSlots = ActivityPageSlots(), pageState: TeakPageScrollState? = null,
    content: @Composable () -> Unit,
) {
    val backRevealed = options.backRevealed
    val showBackButton = options.showBackButton
    val title = options.title
    val limitContentWidth = options.limitContentWidth
    val showTopBar = options.showTopBar
    val showScrollTop = options.showScrollTop
    val titleAlwaysVisible = options.titleAlwaysVisible
    val barBlurFraction = options.barBlurFraction
    val topBarGlyphColor = options.topBarGlyphColor
    val showBackgroundDisc = disc.showBackgroundDisc
    val discLift = disc.discLift
    val discEndPadding = disc.discEndPadding
    val topBarLeading = slots.topBarLeading
    val topBarCenter = slots.topBarCenter
    val topBarActions = slots.topBarActions
    val overlay = slots.overlay
    val menuOverlay = slots.menuOverlay
    PageChromeScope {
    val discContext = LocalContext.current
    TeakBareTheme(settings) { _, materialColors ->
        TeakPageScaffold(
            title = title,
            limitContentWidth = limitContentWidth,
            showTopBar = showTopBar,
            onBack = onBack,
            backRevealed = backRevealed,
            showBackButton = showBackButton,
            pageState = pageState ?: com.aritxonly.teak.common.chrome.rememberTeakPageScrollState(),
            backgroundColor = materialColors.background,
            contentModifier = Modifier
                .background(materialColors.background)
                .smartRefresh(settings.smartRefresh),
            topBarLeading = topBarLeading,
            topBarCenter = topBarCenter,
            topBarActions = topBarActions,
            barBlurFraction = barBlurFraction,
            topBarMaterialProgress = options.topBarMaterialProgress,
            topBarGlyphColor = topBarGlyphColor,
            overlay = {
                if (showScrollTop) ScrollTopButton(settings)
                overlay()
                // 后台播放时，除了播放器页，其他任意页面也保留一张唱片。
                if (!com.aritxonly.teak.common.chrome.isWideFoldTopBar() && showBackgroundDisc &&
                    settings.showDiscAfterVideo &&
                    BackgroundPlayback.active
                ) {
                    // 和首页完全同一份位置计算。
                    val discPlacement = rememberDiscPlacement(settings)
                    BackgroundPlayDisc(
                        settings = settings, dependencies = com.aritxonly.bdfun.AppGraph.discDependencies,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = discEndPadding ?: discPlacement.endPadding)
                            .graphicsLayer {
                                // 外壳自带 8dp 透明留白，下移后视觉底边和首页一致。
                                translationY = discPlacement.bottomOffset.toPx() -
                                    (if (com.aritxonly.teak.common.theme.TeakUiStyle.isHyperOs4) 0.dp else discLift).toPx()
                            },
                        size = discPlacement.size,
                    ) { video ->
                        navigateToVideo(discContext, video)
                    }
                }
            },
            menuOverlay = menuOverlay,
            titleAlwaysVisible = titleAlwaysVisible,
            content = content,
        )
    }
    }
}

/**
 * 只注入主题、不套页面 chrome 的外壳。
 * 开始页 / 引导页要整屏铺自己的背景和顶栏，用这个。
 */
@Composable
fun TeakBareTheme(
    settings: SettingsBindings,
    content: @Composable (dark: Boolean, materialColors: androidx.compose.material3.ColorScheme) -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val lightColors = remember(settings.colorStyle) { hyperLightColors() }
    val darkColors = remember(settings.colorStyle) { hyperDarkColors() }
    val materialColors = appMaterialColors(dark)
    androidx.compose.runtime.SideEffect {
        com.aritxonly.teak.common.theme.TeakUiStyle.style = settings.interfaceStyle
    }
    TeakTheme(
        dark = dark,
        lightColors = lightColors,
        darkColors = darkColors,
        materialColors = materialColors,
        blurEnabled = settings.blurEnabled,
        refractionEnabled = settings.refractionEnabled,
        fontScale = settings.fontScale,
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings provides settings,
            LocalRichTextDependencies provides com.aritxonly.bdfun.AppGraph.richTextDependencies,
        ) { content(dark, materialColors) }
    }
}
