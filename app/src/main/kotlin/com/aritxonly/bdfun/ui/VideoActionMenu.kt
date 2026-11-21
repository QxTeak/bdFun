package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.aritxonly.teak.common.modal.TeakVideoActionMenu
import com.aritxonly.teak.common.modal.TeakVideoMenuItem

/** app 侧沿用原调用名，实现与首页共用同一套 TeakUI 下拉菜单。 */
internal data class VideoMenuItem(
    val label: String,
    val enabled: Boolean = true,
    val danger: Boolean = false,
    val onClick: () -> Unit,
)

@Composable
internal fun VideoActionMenu(
    visible: Boolean,
    anchor: Rect,
    onDismiss: () -> Unit,
    items: List<VideoMenuItem>,
) {
    TeakVideoActionMenu(
        visible = visible,
        anchorInWindow = anchor,
        onDismiss = onDismiss,
        items = items.map { TeakVideoMenuItem(it.label, it.enabled, it.danger, it.onClick) },
    )
}
