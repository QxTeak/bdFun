package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet
import com.aritxonly.teak.common.modal.AlertDialog
import com.aritxonly.teak.common.modal.TextButton

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.aritxonly.bdfun.core.domain.SessionAction
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageSessionSheets(
    snapshot: com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState,
    menu: MessageSessionMenuStateHolder,
    showContactsFallback: Boolean,
    onRefresh: () -> Unit,
    onReadAll: () -> Unit,
    onClearList: () -> Unit,
    onMutate: (SessionAction) -> Unit,
) {
    val navigate = messageRouteAction()
    val colors = MiuixTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val loading = snapshot.loading
    val busy = snapshot.busy
    TeakActionSheet(menu.showMore, onDismiss = { menu.showMore = false }, title = "消息") {
        ActionSheetRow("刷新消息", !loading && !busy) { menu.showMore = false; onRefresh() }
        // 保留服务端下发的快捷入口，联系人已在一级页顶栏。
        if (showContactsFallback) {
            ActionSheetRow("联系人", !loading && !busy) {
                navigate(com.aritxonly.bdfun.core.navigation.Route.MessageTools("contacts"))
                menu.showMore = false
            }
        }
        val entries = (snapshot.menu + snapshot.outside).distinctBy { Triple(it.type, it.title, it.url) }
            .filterNot { it.type == 8 }
        entries.forEach { item ->
            ActionSheetRow(item.title, !loading && !busy) { messageMenuAction(context, item, menu, navigate) }
        }
        if (entries.none { it.type == 2 }) {
            ActionSheetRow("消息设置", !busy) { navigate(com.aritxonly.bdfun.core.navigation.Route.MessageTools("settings")); menu.showMore = false }
        }
    }
    AlertDialog(
        show = menu.confirmRead,
        onDismissRequest = { if (!busy) menu.confirmRead = false },
        miuixTitle = "一键已读",
        miuixSummary = "是否清除全部新消息提醒？",
        confirmButton = {
            TextButton(onClick = onReadAll, enabled = !busy && !loading, miuixText = "确认标为已读") {
                Text("确认标为已读")
            }
        },
        dismissButton = {
            TextButton(onClick = { menu.confirmRead = false }, enabled = !busy, miuixText = "取消") {
                Text("取消")
            }
        },
    )
    TeakActionSheet(menu.confirmClear, onDismiss = { if (!busy) menu.confirmClear = false }, title = "清空列表") {
        Text("清空后所有消息将被删除，无法恢复。", Modifier.padding(18.dp), color = colors.onBackgroundVariant)
        ActionSheetRow("确认清空", !busy && !loading, MiuixIcons.Delete) { onClearList() }
    }
    TeakActionSheet(menu.selected != null, onDismiss = { if (!busy) menu.selected = null }, title = menu.selected?.name) {
        val target = menu.selected
        if (target != null) {
            val enabled = !busy && !loading
            if (menu.confirmDelete) {
                Text("删除会话后，可通过对方的个人空间重新发起私信。", Modifier.padding(18.dp), color = colors.onBackgroundVariant)
                ActionSheetRow("确认删除会话", enabled, MiuixIcons.Delete) { onMutate(SessionAction.Delete) }
                ActionSheetRow("取消", enabled) { menu.confirmDelete = false }
            } else {
                ActionSheetRow(if (target.pinned) "取消置顶" else "置顶会话", enabled) {
                    onMutate(SessionAction.Pin)
                }
                if (target.sessionType == 1) ActionSheetRow(if (target.muted) "关闭免打扰" else "消息免打扰", enabled) {
                    onMutate(SessionAction.Mute)
                }
                if (target.talkerId > 0) ActionSheetRow("标为已读", enabled && (target.unread > 0 || target.preferences.unreadDot)) {
                    onMutate(SessionAction.Read)
                }
                if (target.talkerId > 0) ActionSheetRow("删除会话", enabled, MiuixIcons.Delete) { menu.confirmDelete = true }
            }
        }
    }
}
