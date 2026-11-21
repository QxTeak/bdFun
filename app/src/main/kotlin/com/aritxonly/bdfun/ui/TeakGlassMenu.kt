package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.modal.TeakAnchoredGlassMenu

/**
 * 设置页二级菜单同款的 Miuix 玻璃下拉菜单，可锚定到任意按钮 / 评论位置。
 *
 * 窗口、坐标换算与退场保活已统一在 [TeakAnchoredGlassMenu]；这里保留原调用名与参数。
 */
@Composable
fun TeakGlassMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    anchorInWindow: Rect,
    modifier: Modifier = Modifier,
    minWidth: Dp = 180.dp,
    maxWidth: Dp = 300.dp,
    maxHeight: Dp = 460.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    TeakAnchoredGlassMenu(
        visible = visible,
        onDismiss = onDismiss,
        anchorInWindow = anchorInWindow,
        modifier = modifier,
        minWidth = minWidth,
        maxWidth = maxWidth,
        maxHeight = maxHeight,
        content = content,
    )
}
