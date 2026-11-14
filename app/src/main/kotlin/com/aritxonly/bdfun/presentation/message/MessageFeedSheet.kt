package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.runtime.Composable

@Composable
internal fun MessageFeedSheet(
    kind: MessageFeedKind,
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageFeedUiState,
    menu: MessageFeedMenuStateHolder,
    onMutate: (Boolean) -> Unit,
) {
    val busy = snapshot.busy
    val loadingByTab = snapshot.loadingByTab
    if (kind == MessageFeedKind.Reply || kind == MessageFeedKind.Like) {
        val target = menu.selected
        val enabled = target != null && !busy && loadingByTab[target.first] != true
        com.aritxonly.teak.common.modal.AlertDialog(
            show = target != null,
            onDismissRequest = { if (!busy) menu.selected = null },
            miuixTitle = "删除通知？",
            miuixSummary = if (kind == MessageFeedKind.Like)
                "删除后，有新的点赞时这条通知会重新出现在列表。" else "确认删除这条通知？",
            confirmButton = {
                com.aritxonly.teak.common.modal.TextButton(
                    onClick = { onMutate(true) }, enabled = enabled, miuixText = "删除",
                ) { androidx.compose.material3.Text("删除") }
            },
            dismissButton = {
                com.aritxonly.teak.common.modal.TextButton(
                    onClick = { menu.selected = null }, enabled = !busy, miuixText = "取消",
                ) { androidx.compose.material3.Text("取消") }
            },
        )
        return
    }
    TeakActionSheet(menu.selected != null, onDismiss = { if (!busy) menu.selected = null }, title = "消息操作") {
        val target = menu.selected
        if (target != null) {
            val enabled = !busy && loadingByTab[target.first] != true
            if (menu.confirmMute) {
                androidx.compose.material3.Text("这条内容的点赞将不再通知，但仍可在列表中查看。",
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackgroundVariant)
                ActionSheetRow("确认不再通知", enabled) { onMutate(false) }
                ActionSheetRow("取消", enabled) { menu.confirmMute = false }
            } else if (menu.confirmDelete) {
                if (kind == MessageFeedKind.Like) androidx.compose.material3.Text(
                    "删除后，有新的点赞时这条通知会重新出现在列表。",
                    color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackgroundVariant)
                ActionSheetRow("确认删除这条通知", enabled, MiuixIcons.Delete) {
                    onMutate(true)
                }
                ActionSheetRow("取消", enabled) { menu.confirmDelete = false }
            } else {
                if (kind == MessageFeedKind.Like) ActionSheetRow(if (target.second.noticeState == 0) "不再通知" else "接收通知", enabled) {
                    if (target.second.noticeState == 0) menu.confirmMute = true else onMutate(false)
                }
                ActionSheetRow("删除通知", enabled, MiuixIcons.Delete) { menu.confirmDelete = true }
            }
        }
    }
}
