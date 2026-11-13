package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.modal.TeakActionSheet

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
        snapshot.menu.forEach { item ->
            ActionSheetRow(item.title, !loading && !busy) { messageMenuAction(context, item, menu, navigate) }
        }
        if (snapshot.menu.isEmpty()) {
            ActionSheetRow("消息设置", !busy) { navigate(com.aritxonly.bdfun.core.navigation.Route.MessageTools("settings")); menu.showMore = false }
        }
    }
    TeakActionSheet(menu.confirmRead, onDismiss = { if (!busy) menu.confirmRead = false }, title = "一键已读") {
        Text("是否清除全部新消息提醒？", Modifier.padding(18.dp), color = colors.onBackgroundVariant)
        ActionSheetRow("确认标为已读", !busy && !loading) { onReadAll() }
    }
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
