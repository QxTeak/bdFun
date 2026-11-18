package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.ChatPreferenceAction
import com.aritxonly.bdfun.feature.message.application.*
import com.aritxonly.teak.common.modal.TeakActionSheet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private class ChatSettingsPresentation {
    var confirmation by mutableStateOf<ChatPreferenceAction?>(null)
}

@Composable
internal fun MessageChatSettings(mid: Long, state: MessageToolsUiState, controller: MessageToolsStateHolder, scope: CoroutineScope) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    val ui = remember(controller) { ChatSettingsPresentation() }
    val chat = state.chat ?: return
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(Modifier.fillMaxWidth().clickable { com.aritxonly.bdfun.navigateToSpace(context, mid) }.padding(vertical = 16.dp)) {
            MessageAvatar(chat.avatar); Spacer(Modifier.width(12.dp)); Text(chat.name, color = colors.onBackground)
        }
        val pinned = chat.pinned
        if (pinned == null) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("置顶聊天", color = colors.onBackground)
                    Text(chat.pinError ?: "置顶状态未知", color = colors.onBackgroundVariant)
                }
                Text("重试", color = colors.primary, modifier = Modifier.clickable(
                    enabled = !state.loading && !state.busy,
                ) { scope.launch { controller.load() } }.padding(12.dp))
            }
        }
        val toggles = listOfNotNull(
            pinned?.let { Triple("置顶聊天", it, ChatPreferenceAction.Pin) },
            Triple("消息免打扰", chat.muted, ChatPreferenceAction.Mute),
            Triple("加入黑名单", chat.blocked, ChatPreferenceAction.Block),
        ) +
            (chat.push?.let { listOf(Triple("内容推送", it, ChatPreferenceAction.Push)) } ?: emptyList())
        toggles.forEach { (label, value, action) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(label, color = colors.onBackground, modifier = Modifier.weight(1f))
                top.yukonga.miuix.kmp.basic.Switch(value, {
                    if (action == ChatPreferenceAction.Block && !value || action == ChatPreferenceAction.Push && value) ui.confirmation = action
                    else scope.launch { controller.chatPreference(mid, action, !value)?.let(context::showToast) }
                }, enabled = !state.busy && !state.loading)
            }
        }
    }
    TeakActionSheet(ui.confirmation != null, onDismiss = { if (!state.busy) ui.confirmation = null }, title = "确认设置") {
        Text(if (ui.confirmation == ChatPreferenceAction.Block) "拉黑将解除关注和合集订阅，禁止该用户与你互动或查看你的空间。"
            else "关闭后不再收到该账号的图文与稿件推送，通知类消息不受影响。", color = colors.onBackgroundVariant)
        ActionSheetRow("确认", !state.busy) {
            val action = ui.confirmation ?: return@ActionSheetRow
            scope.launch { controller.chatPreference(mid, action, action == ChatPreferenceAction.Block)?.let(context::showToast); ui.confirmation = null }
        }
    }
}
