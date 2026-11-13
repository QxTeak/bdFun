package com.aritxonly.teak.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import com.kyant.shapes.Capsule

/** 仅由页面壳提供滚动收起进度，组件不持有页面滚动状态。 */
val LocalTeakDockScrollFraction = compositionLocalOf { 0f }

object TeakFloatingTabBarDefaults {
    val Height = 52.dp
    val CollapsedHeight = 40.dp
    val CurrentHeight @Composable get() = lerp(Height, CollapsedHeight, LocalTeakDockScrollFraction.current.coerceIn(0f, 1f))
    val HorizontalContentPadding = 6.dp
    val VerticalContentPadding = 3.dp
    val IndicatorHorizontalOverflow = 3.dp
    val MaximumWidth = 320.dp
    val MaximumItemWidth = 72.dp
    val TabIconSize = 26.dp
    val DetachedIconSize @Composable get() = if (isWideFoldTopBar()) 25.dp else 28.dp
    val DetachedSpacing @Composable get() = if (isWideFoldTopBar()) 12.dp else 8.dp
    val DockHorizontalPadding = 8.dp
    val ContainerShape = Capsule()
    val IndicatorShape = Capsule()
}
