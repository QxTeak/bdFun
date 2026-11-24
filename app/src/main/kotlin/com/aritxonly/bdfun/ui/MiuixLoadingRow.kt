package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.aritxonly.teak.common.chrome.TeakLoadingRow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 统一的加载占位：Miuix 默认尺寸（20dp）的「环形 + 环绕圆点」加载动画。
 *
 * 不再额外放大、也不加文字；具体实现放在 TeakUI，供 feature 模块共用。
 */
@Composable
internal fun MiuixLoadingRow(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    verticalPadding: Dp = 36.dp,
) {
    TeakLoadingRow(modifier = modifier, size = size, verticalPadding = verticalPadding)
}

/** 首屏加载：固定在整屏中间，不随列表头/顶栏高度上浮。 */
@Composable
internal fun PageCenteredLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        MiuixLoadingRow(verticalPadding = 0.dp)
    }
}
