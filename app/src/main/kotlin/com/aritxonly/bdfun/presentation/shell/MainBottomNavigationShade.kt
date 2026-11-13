package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakUiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 仅为竖屏悬浮底栏提供内容渐隐；与底栏共用显隐层，不进入玻璃采样树。 */
@Composable
internal fun BoxScope.MainBottomNavigationShade(
    settings: SettingsBindings, wideNavigation: Boolean, barAlpha: Float,
) {
    val window = LocalWindowInfo.current.containerSize
    if (window.height <= window.width || (!wideNavigation && TeakUiStyle.isClassic)) return
    val navigationBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomSpacing = if (isWideFoldTopBar()) 0.dp else 4.dp
    val lift = if (!wideNavigation && settings.bottomBarRaise) TeakChromeDefaults.RaisedBottomBarExtra else 0.dp
    val height = navigationBottom + bottomSpacing + lift + TeakFloatingTabBarDefaults.CurrentHeight + 24.dp
    val background = MiuixTheme.colorScheme.background
    Box(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(height)
            .graphicsLayer { alpha = barAlpha }
            .background(Brush.verticalGradient(listOf(background.copy(alpha = 0f), background))),
    )
}
