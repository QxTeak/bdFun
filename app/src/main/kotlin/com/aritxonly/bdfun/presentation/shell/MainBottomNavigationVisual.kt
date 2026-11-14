package com.aritxonly.bdfun


import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.UiSwapDurationMs
import com.aritxonly.bdfun.ui.rememberManualProgress
import com.aritxonly.bdfun.ui.rememberSpringFloat
import com.aritxonly.bdfun.ui.BackgroundPlayback
import com.aritxonly.bdfun.ui.rememberDiscPlacement

internal data class MainBottomNavigationVisual(
    val tabBarCovered: Boolean, val tabBarAlpha: Float, val barSelectedColor: Color,
    val discSlot: Float, val discShift: androidx.compose.ui.unit.Dp,
    val discEndPadding: androidx.compose.ui.unit.Dp, val discBottomOffset: androidx.compose.ui.unit.Dp,
)

@Composable
internal fun rememberMainBottomNavigationVisual(
    settings: SettingsBindings, dark: Boolean, wideNavigation: Boolean, searchVisible: Boolean, itemCount: Int,
): MainBottomNavigationVisual {
    // 视频详情页隐藏底栏，避免遮挡播放器控件
    val tabBarCovered = searchVisible
    val tabBarAlpha = rememberManualProgress(
        if (tabBarCovered) 0f else 1f,
        UiSwapDurationMs,
    )
    val barSelectedColor = if (settings.colorStyle == 1) {
        if (dark) Color.White else Color(0xFF1B1C1F)
    } else {
        BrandPink
    }

    val discVisible = !wideNavigation && settings.showDiscAfterVideo && BackgroundPlayback.active
    val discSlot = rememberSpringFloat(
        target = if (discVisible) 1f else 0f,
        stiffness = 300f,
        damping = 26f,
    )
    // 底部唱片出现时，底栏本身的高度和宽度完全不动，只是整体向左让位。
    //
    // 底栏不是满屏宽：三个 Tab 每个最多 88dp，所以它只有 264dp。
    // 有唱片时，底栏 + 8dp 缝隙 + 唱片作为“一组”整体居中，
    // 底栏离屏幕左边的距离 = 唱片离屏幕右边的距离。
    val windowWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val tabBarWidth = minOf(
        TeakFloatingTabBarDefaults.MaximumWidth,
        TeakFloatingTabBarDefaults.MaximumItemWidth * itemCount,
        (windowWidthDp - 32.dp).coerceAtLeast(0.dp),
    )
    val discWidth = TeakFloatingTabBarDefaults.Height
    val discPlacement = rememberDiscPlacement(settings, itemCount)
    val discGap = 8.dp
    // 整组居中后的左留白。
    val groupLeft = ((windowWidthDp - tabBarWidth - discGap - discWidth) / 2)
        .coerceAtLeast(0.dp)
    val discShift = if (settings.interfaceStyle == 2) {
        0.dp
    } else if (TeakUiStyle.isClassic) {
        // 经典样式底栏是整条贴边，只能整体左移，让出唱片 + 右边距 + 缝隙。
        discWidth + 16.dp + discGap
    } else {
        // 底栏从“自己居中”变成“跟着唱片一起居中”，只往左让半个唱片位。
        (windowWidthDp - tabBarWidth) / 2 - groupLeft
    }
    // 所有页面的唱片位置统一由 rememberDiscPlacement 计算，保证和首页一致。
    val discEndPadding = if (wideNavigation) 16.dp else discPlacement.endPadding
    val discBottomOffset = discPlacement.bottomOffset

    return MainBottomNavigationVisual(
        tabBarCovered, tabBarAlpha, barSelectedColor, discSlot, discShift, discEndPadding, discBottomOffset,
    )
}
