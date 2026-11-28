package com.aritxonly.bdfun.ui

import top.yukonga.miuix.kmp.glass.GlassPopupItem

import com.aritxonly.teak.common.modal.TeakActionSheet
import com.aritxonly.teak.common.modal.AlertDialog
import com.aritxonly.teak.common.modal.TextButton

import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Delete
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import com.aritxonly.bdfun.core.domain.SessionAction
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageSessionSheets(
    snapshot: MessageSessionsUiState,
    menu: MessageSessionMenuStateHolder,
    showContactsFallback: Boolean,
    onRefresh: () -> Unit,
    onReadAll: () -> Unit,
    onClearList: () -> Unit,
    onMutate: (SessionAction) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val loading = snapshot.loading
    val busy = snapshot.busy
    MessageInboxDropdown(snapshot, menu, showContactsFallback, onRefresh)
    AlertDialog(
        show = menu.confirmRead,
        onDismissRequest = { if (!busy) menu.confirmRead = false },
        miuixTitle = "一键已读",
        miuixSummary = "是否清除全部新消息提醒？",
        confirmButton = {
            TextButton(onClick = onReadAll, enabled = !busy && !loading, miuixText = "确认标为已读",
                primary = true) {
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
    MessagePrivateSessionMenu(menu, enabled = !busy && !loading, onMutate = onMutate)
}

@Composable
private fun MessagePrivateSessionMenu(
    menu: MessageSessionMenuStateHolder,
    enabled: Boolean,
    onMutate: (SessionAction) -> Unit,
) {
    val target = menu.selected
    val dismiss = {
        menu.sessionMenuVisible = false
        menu.selected = null
    }
    val select: (SessionAction) -> Unit = {
        menu.sessionMenuVisible = false
        onMutate(it)
    }
    TeakGlassMenu(
        visible = menu.sessionMenuVisible && target != null,
        onDismiss = dismiss,
        anchorInWindow = menu.sessionAnchor,
    ) {
        if (target != null) {
            GlassPopupItem(
                text = if (target.pinned) "取消置顶" else "置顶会话", enabled = enabled,
                onClick = { select(SessionAction.Pin) },
            )
            if (target.sessionType == 1) {
                GlassPopupItem(
                    text = if (target.muted) "关闭免打扰" else "消息免打扰", enabled = enabled,
                    onClick = { select(SessionAction.Mute) },
                )
            }
            if (target.talkerId > 0) {
                GlassPopupItem(
                    text = "标为已读",
                    enabled = enabled && (target.unread > 0 || target.preferences.unreadDot),
                    onClick = { select(SessionAction.Read) },
                )
                GlassPopupItem(text = "删除会话", enabled = enabled, onClick = {
                    menu.sessionMenuVisible = false
                    menu.confirmDelete = true
                })
            }
        }
    }
    AlertDialog(
        show = menu.confirmDelete && target != null,
        onDismissRequest = { if (enabled) { menu.confirmDelete = false; dismiss() } },
        miuixTitle = "删除会话？",
        miuixSummary = "删除会话后，可通过对方的个人空间重新发起私信。",
        confirmButton = {
            TextButton(onClick = { onMutate(SessionAction.Delete) }, enabled = enabled,
                miuixText = "删除", destructive = true) { Text("删除") }
        },
        dismissButton = {
            TextButton(onClick = { menu.confirmDelete = false; dismiss() }, enabled = enabled,
                miuixText = "取消") { Text("取消") }
        },
    )
}

/**
 * 消息页“更多”菜单的内容登记：宿主画在 Activity 的 menuOverlay 槽（页面采集树之外），
 * 这里只把当前数据交出去，每次重组刷新，保证打开时读到的 enabled 与条目是最新的。
 */
@Composable
private fun MessageInboxDropdown(
    snapshot: MessageSessionsUiState,
    menu: MessageSessionMenuStateHolder,
    showContactsFallback: Boolean,
    onRefresh: () -> Unit,
) {
    val chrome = LocalPageChromeState.current
    val navigate = messageRouteAction()
    SideEffect {
        chrome.moreMenuContent = { dismiss ->
            MessageInboxMenuContent(snapshot, menu, showContactsFallback, onRefresh, navigate, dismiss)
        }
    }
    // 页面离开时把宿主上的旧条目一起收掉，避免菜单停在上一个页面的数据上。
    DisposableEffect(chrome) {
        onDispose {
            chrome.moreMenu.close()
            chrome.moreMenuContent = null
        }
    }
}

@Composable
private fun MessageInboxMenuContent(
    snapshot: MessageSessionsUiState,
    menu: MessageSessionMenuStateHolder,
    showContactsFallback: Boolean,
    onRefresh: () -> Unit,
    navigate: (Route) -> Unit,
    dismiss: (after: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val loading = snapshot.loading
    val busy = snapshot.busy
    GlassPopupItem(
        text = "刷新消息",
        enabled = !loading && !busy,
        onClick = { dismiss { onRefresh() } },
    )
    // 保留服务端下发的快捷入口，联系人已在一级页顶栏。
    if (showContactsFallback) {
        GlassPopupItem(text = "联系人", enabled = !loading && !busy, onClick = {
            dismiss { navigate(Route.MessageTools("contacts")) }
        })
    }
    val entries = (snapshot.menu + snapshot.outside).distinctBy { Triple(it.type, it.title, it.url) }
        .filterNot { it.type == 8 }
    entries.forEach { item ->
        GlassPopupItem(
            text = item.title,
            enabled = !loading && !busy,
            onClick = { dismiss { messageMenuAction(context, item, menu, navigate) } },
        )
    }
    if (entries.none { it.type == 2 }) {
        GlassPopupItem(text = "消息设置", enabled = !busy, onClick = {
            dismiss { navigate(Route.MessageTools("settings")) }
        })
    }
}
