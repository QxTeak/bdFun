package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
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
