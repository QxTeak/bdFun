package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.glass.GlassPopupItem

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.message.application.MessageFeedUiState
import com.aritxonly.teak.common.modal.AlertDialog
import com.aritxonly.teak.common.modal.TextButton

@Composable
internal fun MessageFeedSheet(
    kind: MessageFeedKind,
    snapshot: MessageFeedUiState,
    menu: MessageFeedMenuStateHolder,
    onMutate: (Boolean) -> Unit,
) {
    val target = menu.selected
    val enabled = target != null && !snapshot.busy && snapshot.loadingByTab[target.first] != true
    TeakGlassMenu(
        visible = menu.showMenu && target != null,
        onDismiss = { menu.showMenu = false; menu.selected = null },
        anchorInWindow = menu.anchor,
        minWidth = 148.dp,
    ) {
        // 点赞接口支持通知开关，回复和系统通知只有删除能力。
        if (kind == MessageFeedKind.Like && target != null) {
            GlassPopupItem(
                text = if (target.second.noticeState == 0) "不再通知" else "接收通知",
                enabled = enabled,
                onClick = {
                    menu.showMenu = false
                    onMutate(false)
                },
            )
        }
        GlassPopupItem(text = "删除", enabled = enabled, onClick = {
            menu.showMenu = false
            menu.confirmDelete = true
        })
    }
    AlertDialog(
        show = menu.confirmDelete && target != null,
        onDismissRequest = {
            if (!snapshot.busy) {
                menu.confirmDelete = false
                menu.selected = null
            }
        },
        miuixTitle = "删除通知？",
        miuixSummary = if (kind == MessageFeedKind.Like)
            "删除后，有新的点赞时这条通知会重新出现在列表。" else "确认删除这条通知？",
        confirmButton = {
            TextButton(onClick = { onMutate(true) }, enabled = enabled, miuixText = "删除", destructive = true) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = { menu.confirmDelete = false; menu.selected = null },
                enabled = !snapshot.busy, miuixText = "取消") { Text("取消") }
        },
    )
}
