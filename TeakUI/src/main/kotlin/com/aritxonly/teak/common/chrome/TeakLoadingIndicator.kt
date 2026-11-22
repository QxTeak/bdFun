package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 默认尺寸（20dp）的「环形 + 环绕圆点」加载动画。
 *
 * 不放大、不加文字；列表/详情页的加载占位统一用它，替代原来的骨架屏。
 */
@Composable
fun TeakLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
) {
    InfiniteProgressIndicator(size = size, color = MiuixTheme.colorScheme.onBackgroundVariant, modifier = modifier)
}

/** 列表里的整行加载占位：水平居中并留上下空白。 */
@Composable
fun TeakLoadingRow(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    verticalPadding: Dp = 36.dp,
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        TeakLoadingIndicator(size = size)
    }
}

/**
 * 「内容区域」居中的加载占位：高度等于窗口高度减去顶部栏+状态栏，
 * 所以图标落在顶栏以下那块可见内容的中间，而不是整个屏幕的中间。
 */
@Composable
fun TeakLoadingArea(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    extraTopInset: Dp = 0.dp,
) {
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current
    val topInset = WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding() + TeakChromeDefaults.TopBarHeight + extraTopInset
    val height = with(density) { window.height.toDp() } - topInset
    Box(
        modifier = modifier.fillMaxWidth().height(height.coerceAtLeast(160.dp)),
        contentAlignment = Alignment.Center,
    ) {
        TeakLoadingIndicator(size = size)
    }
}
