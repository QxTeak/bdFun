package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuRuleKind
import com.aritxonly.bdfun.core.domain.danmakuTimestampMs
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun DanmakuSelectionActions(playback: PlaybackController, cid: Long) {
    val page = LocalVideoPageState.current
    val controller = page.danmakuModeration ?: return
    val context = LocalContext.current
    var confirmation by remember(cid) { mutableStateOf<Pair<DanmakuItem, DanmakuMenuAction>?>(null) }
    var followText by remember(cid) { mutableStateOf<String?>(null) }
    LaunchedEffect(controller) { controller.loadRules() }
    fun choose(item: DanmakuItem, action: DanmakuMenuAction) {
        when (action) {
            DanmakuMenuAction.Seek -> playback.seekTo(item.timeMs.coerceAtLeast(0))
            DanmakuMenuAction.Timestamp -> {
                val position = danmakuTimestampMs(item.text, playback.refresh().durationMs)
                if (position != null) playback.seekTo(position)
                else context.showToast("无有效时间链接")
            }
            DanmakuMenuAction.FollowSend -> followText = item.text
            DanmakuMenuAction.Recall, DanmakuMenuAction.Block -> confirmation = item to action
        }
    }

    LaunchedEffect(page.pendingDanmakuAction, cid) {
        val request = page.pendingDanmakuAction ?: return@LaunchedEffect
        page.pendingDanmakuAction = null
        if (request.cid == cid) choose(request.item, request.action)
    }

    DanmakuModerationConfirmation(confirmation, onDismiss = { confirmation = null }) { item, action ->
        val ok = if (action == DanmakuMenuAction.Recall) controller.recall(cid, item)
            else controller.addRule(DanmakuRuleKind.Sender, item.userHash)
        context.showToast(if (ok) "操作成功" else "操作失败")
    }
    DanmakuFollowSendDialog(followText, onDismiss = { followText = null }) { text ->
        val ok = page.cid == cid && page.danmakuSender?.invoke(text, 1, 25, 0xFFFFFF) == true
        if (!ok) context.showToast("发送失败，请确认已登录")
        ok
    }
}

internal data class DanmakuActionRequest(val cid: Long, val item: DanmakuItem, val action: DanmakuMenuAction)

internal enum class DanmakuMenuAction(val title: String) {
    Seek("跳到弹幕"), Timestamp("跳到链接"), FollowSend("发送同款"), Recall("撤回"), Block("屏蔽作者"),
}


@Composable
private fun DanmakuModerationConfirmation(
    confirmation: Pair<DanmakuItem, DanmakuMenuAction>?, onDismiss: () -> Unit,
    confirm: suspend (DanmakuItem, DanmakuMenuAction) -> Unit,
) {
    val (item, action) = confirmation ?: return
    val scope = rememberCoroutineScope()
    var busy by remember(item, action) { mutableStateOf(false) }
    WindowDialog(show = true, title = if (action == DanmakuMenuAction.Recall) "撤回这条弹幕？" else "屏蔽这位作者的弹幕？",
        onDismissRequest = { if (!busy) onDismiss() }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                busy = true
                scope.launch {
                    try { confirm(item, action); onDismiss() }
                    finally { busy = false }
                }
            }) { Text("确认") }
        }
    }
}
