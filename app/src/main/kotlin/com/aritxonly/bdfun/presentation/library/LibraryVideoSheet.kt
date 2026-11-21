package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import com.aritxonly.teak.common.modal.TeakVideoActionMenu
import com.aritxonly.teak.common.modal.TeakVideoMenuItem

/**
 * 收藏 / 历史 / 稍后再看的视频操作菜单。
 *
 * 与首页同一套 Miuix 玻璃下拉：长按卡片或点右下角省略号都走这里；
 * 选项直接复用批量管理（删除 / 移动到 / 复制到），不另写业务。
 */
@Composable
internal fun LibraryVideoMenu(
    show: Boolean,
    anchor: Rect,
    allowTransfer: Boolean,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
) {
    TeakVideoActionMenu(
        visible = show,
        anchorInWindow = anchor,
        onDismiss = onDismiss,
        items = buildList {
            add(TeakVideoMenuItem("删除", danger = true) { onAction("delete") })
            if (allowTransfer) {
                add(TeakVideoMenuItem("移动到收藏夹") { onAction("move") })
                add(TeakVideoMenuItem("复制到收藏夹") { onAction("copy") })
            }
        },
    )
}
