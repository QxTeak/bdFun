package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.DanmakuCloudConfig
import com.aritxonly.bdfun.core.domain.DanmakuReportReason
import com.aritxonly.bdfun.core.domain.DanmakuRuleKind
import com.aritxonly.bdfun.core.domain.danmakuTimestampMs
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
internal fun BoxScope.DanmakuManagementOverlay(
    items: List<DanmakuItem>, playback: PlaybackController, cid: Long, controlsVisible: Boolean,
) {
    val page = LocalVideoPageState.current
    val controller = page.danmakuModeration ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by controller.state.collectAsState()
    val votes = page.danmakuVotes?.collectAsState()?.value.orEmpty()
    var section by remember(cid) { mutableStateOf<DanmakuManagementSection?>(null) }
    var selected by remember(cid) { mutableStateOf<DanmakuItem?>(null) }
    var confirmation by remember(cid) { mutableStateOf<Pair<DanmakuItem, PoolAction>?>(null) }
    var followText by remember(cid) { mutableStateOf<String?>(null) }
    var report by remember(cid) { mutableStateOf<DanmakuItem?>(null) }
    LaunchedEffect(controller) { controller.loadRules() }
    LaunchedEffect(page.inspectDanmaku) {
        val item = page.inspectDanmaku ?: return@LaunchedEffect
        selected = items.firstOrNull { it.id == item.id } ?: item
        section = DanmakuManagementSection.Item
        page.inspectDanmaku = null
    }
    LaunchedEffect(selected?.id, cid) {
        selected?.id?.takeIf { it > 0 }?.let { page.danmakuVoteLoader?.invoke(listOf(it)) }
    }
    if (controlsVisible) DanmakuPoolEntry { section = DanmakuManagementSection.Pool }

    fun choose(item: DanmakuItem, action: PoolAction) {
        when (action) {
            PoolAction.Seek -> { playback.seekTo(item.timeMs.coerceAtLeast(0)); section = null }
            PoolAction.Timestamp -> {
                val position = danmakuTimestampMs(item.text, playback.refresh().durationMs)
                if (position != null) { playback.seekTo(position); section = null }
                else context.showToast("无有效时间链接")
            }
            PoolAction.Copy -> {
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("弹幕", item.text))
                context.showToast("已复制")
            }
            PoolAction.Like -> {
                scope.launch {
                    if (page.danmakuLiker?.invoke(item.id) != true) context.showToast("点赞操作失败")
                }
            }
            PoolAction.Report -> report = item
            PoolAction.FollowSend -> followText = item.text
            PoolAction.Recall, PoolAction.Block -> confirmation = item to action
        }
    }

    MiuixBottomSheet(show = section != null, title = section?.title, onDismissRequest = { section = null; selected = null }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (section) {
                DanmakuManagementSection.Rules -> DanmakuRulesPanel(state.rules,
                    onBack = { section = DanmakuManagementSection.Pool },
                    add = { kind, value -> controller.addRule(kind, value) }, remove = { controller.deleteRule(it) },
                    sync = { controller.syncConfig(DanmakuCloudConfig.from(page.settings)) })
                DanmakuManagementSection.Pool -> DanmakuPoolList(items, cid, state.recalled, selected, votes,
                    openRules = { selected = null; section = DanmakuManagementSection.Rules },
                    onSelect = { selected = if (selected == it) null else it }, onAction = ::choose)
                DanmakuManagementSection.Item -> selected?.let { item ->
                    DanmakuPoolRow(item, votes[cid to item.id], true, {}, action = { choose(item, it) })
                }
                null -> Unit
            }
        }
    }
    DanmakuModerationConfirmation(confirmation, onDismiss = { confirmation = null }) { item, action ->
        val ok = if (action == PoolAction.Recall) controller.recall(cid, item)
            else controller.addRule(DanmakuRuleKind.Sender, item.userHash)
        if (ok && section == DanmakuManagementSection.Item) section = null
        context.showToast(if (ok) "操作成功" else "操作失败")
    }
    DanmakuManagementDialogs(followText, report, onDismissFollow = { followText = null },
        onDismissReport = { report = null }, page)

}

private enum class DanmakuManagementSection(val title: String) { Pool("弹幕池"), Rules("云端屏蔽规则"), Item("弹幕操作") }
internal enum class PoolAction(val title: String) {
    Seek("跳到弹幕"), Timestamp("跳到链接"), Copy("复制"), Like("点赞"), FollowSend("发送同款"), Report("举报"), Recall("撤回"), Block("屏蔽作者"),
}


@Composable
private fun DanmakuModerationConfirmation(
    confirmation: Pair<DanmakuItem, PoolAction>?, onDismiss: () -> Unit,
    confirm: suspend (DanmakuItem, PoolAction) -> Unit,
) {
    val (item, action) = confirmation ?: return
    val scope = rememberCoroutineScope()
    var busy by remember(item, action) { mutableStateOf(false) }
    WindowDialog(show = true, title = if (action == PoolAction.Recall) "撤回这条弹幕？" else "屏蔽这位作者的弹幕？",
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


@Composable
private fun BoxScope.DanmakuPoolEntry(open: () -> Unit) {
    Text("弹幕池", color = Color.White, fontSize = 12.sp,
        modifier = Modifier.align(Alignment.TopEnd).padding(top = 52.dp, end = 12.dp)
            .background(Color.Black.copy(alpha = 0.52f), RoundedCornerShape(16.dp))
            .clickable(onClick = open).padding(horizontal = 12.dp, vertical = 7.dp))
}


@Composable
private fun DanmakuManagementDialogs(
    followText: String?, report: DanmakuItem?, onDismissFollow: () -> Unit, onDismissReport: () -> Unit,
    page: VideoPagePresentationState,
) {
    val context = LocalContext.current
    DanmakuFollowSendDialog(followText, onDismiss = onDismissFollow) { text ->
        val ok = page.danmakuSender?.invoke(text, 1, 25, 0xFFFFFF) == true
        if (!ok) context.showToast("发送失败，请确认已登录")
        ok
    }
    report?.let { item ->
        DanmakuReportDialog(item, onDismiss = onDismissReport) { reason: DanmakuReportReason, text ->
            val ok = page.danmakuReporter?.invoke(item.id, reason, text) == true
            context.showToast(if (ok) "举报已提交" else "举报失败")
            ok
        }
    }
}
