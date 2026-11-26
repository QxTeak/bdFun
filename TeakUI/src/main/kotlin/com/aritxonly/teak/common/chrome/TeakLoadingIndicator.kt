package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Miuix 默认尺寸（20dp）的「环形 + 环绕圆点」加载动画。
 *
 * 页面/列表级占位统一带一行加载文案；按钮、卡片内的小图标传 `label = null` 保持紧凑。
 */
@Composable
fun TeakLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    label: String? = null,
) {
    if (label == null) {
        InfiniteProgressIndicator(
            size = size,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            modifier = modifier,
        )
        return
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        InfiniteProgressIndicator(
            size = size,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 12.sp,
        )
    }
}

/** 列表里的整行加载占位：水平居中并留上下空白。 */
@Composable
fun TeakLoadingRow(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    verticalPadding: Dp = 36.dp,
    label: String? = "正在加载…",
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = verticalPadding),
        contentAlignment = Alignment.Center,
    ) {
        TeakLoadingIndicator(size = size, label = label)
    }
}

/**
 * 首屏加载占位：图标固定在**整个窗口的中心**。
 *
 * 它作为列表项出现，列表的 contentPadding 会把这一项整体往下推；这里用自己实际落在窗口里的
 * y 坐标反推偏移量，所以不管顶栏/tab/留白有多少，图标都在屏幕正中（`extraTopInset` 已不再需要，
 * 仅为调用方源码兼容保留）。
 */
@Composable
fun TeakLoadingArea(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    @Suppress("UNUSED_PARAMETER") extraTopInset: Dp = 0.dp,
    label: String? = "正在加载…",
) {
    val density = LocalDensity.current
    val windowHeightPx = LocalWindowInfo.current.containerSize.height
    val areaHeight = 160.dp
    val areaHeightPx = with(density) { areaHeight.toPx() }
    var itemTopPx by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(areaHeight)
            .onGloballyPositioned { coordinates -> itemTopPx = coordinates.positionInWindow().y }
            .offset {
                if (itemTopPx <= 0f) {
                    IntOffset.Zero
                } else {
                    IntOffset(0, (windowHeightPx / 2f - itemTopPx - areaHeightPx / 2f).roundToInt())
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        TeakLoadingIndicator(size = size, label = label)
    }
}
