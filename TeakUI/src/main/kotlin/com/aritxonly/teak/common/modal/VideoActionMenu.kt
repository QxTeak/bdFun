package com.aritxonly.teak.common.modal

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.glass.GlassPopupItem

/** 视频卡片长按 / 省略号共用的下拉菜单项。 */
data class TeakVideoMenuItem(
    val label: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

/**
 * 视频操作下拉菜单（Miuix 玻璃）。
 *
 * 外壳复用 [TeakAnchoredGlassMenu]；这里只负责把业务项映射成玻璃菜单行。
 */
@Composable
fun TeakVideoActionMenu(
    visible: Boolean,
    anchorInWindow: Rect,
    onDismiss: () -> Unit,
    items: List<TeakVideoMenuItem>,
) {
    TeakAnchoredGlassMenu(
        visible = visible,
        onDismiss = onDismiss,
        anchorInWindow = anchorInWindow,
        minWidth = 168.dp,
        maxWidth = 236.dp,
        maxHeight = 420.dp,
    ) {
        items.forEach { item ->
            GlassPopupItem(
                text = item.label,
                onClick = { if (item.enabled) item.onClick() },
                contentColor = if (item.danger) Color(0xFFE5484D) else Color.Unspecified,
                selected = false,
            )
        }
    }
}
