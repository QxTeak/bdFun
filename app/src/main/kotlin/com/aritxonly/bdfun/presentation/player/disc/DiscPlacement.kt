package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings

internal data class DiscPlacement(
    val endPadding: Dp,
    val bottomOffset: Dp,
    val size: Dp,
)

@Composable
internal fun rememberDiscPlacement(settings: SettingsBindings, tabCount: Int = 3, stackedInSearch: Boolean = true): DiscPlacement {
    val windowWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val classic = TeakUiStyle.isClassic
    val tabBarWidth = minOf(
        TeakFloatingTabBarDefaults.MaximumWidth,
        TeakFloatingTabBarDefaults.MaximumItemWidth * tabCount,
        (windowWidthDp - 32.dp).coerceAtLeast(0.dp),
    )
    val discWidth = TeakFloatingTabBarDefaults.Height
    val discGap = 8.dp
    val groupLeft = ((windowWidthDp - tabBarWidth - discGap - discWidth) / 2)
        .coerceAtLeast(0.dp)
    if (TeakUiStyle.isHyperOs4 && stackedInSearch) {
        // 首页/主壳的唱片叠进搜索位，由 MainDockStack 自己定位，这里的值不参与绘制。
        return DiscPlacement(endPadding = 0.dp, bottomOffset = (-70).dp, size = discWidth)
    }
    // 首页外的页面不再单独贴右下角：B 站布局和 HyperOS 4 都复用首页同一组几何。
    // HyperOS 4 首页的底栏内容（Tab + 8dp 缝 + 52dp 搜索/唱片槽）与 B 站首页的
    // 唱片 + 底栏在同一宽度约束下居中，所以两套布局算出的绝对位置本来就一致。
    val endPadding = if (classic) 8.dp else (groupLeft - 8.dp).coerceAtLeast(0.dp)
    val bottomOffset = if (classic) {
        // 经典底栏高 58dp。唱片不嵌进栏里——那会把三个 Tab 挤成非等分——
        // 而是浮在栏上方留 8dp 缝隙，底栏和 Tab 保持原样。
        (-(58 + 8)).dp
    } else {
        4.dp - if (settings.bottomBarRaise) {
            TeakChromeDefaults.RaisedBottomBarExtra
        } else {
            0.dp
        }
    }
    return DiscPlacement(
        endPadding = endPadding,
        bottomOffset = bottomOffset,
        size = discWidth,
    )
}
