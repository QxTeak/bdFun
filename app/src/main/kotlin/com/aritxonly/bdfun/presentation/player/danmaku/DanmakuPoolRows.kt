package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.domain.DanmakuCloudRule
import com.aritxonly.bdfun.core.domain.DanmakuVote
import com.aritxonly.bdfun.core.domain.DanmakuReportReason
import com.aritxonly.bdfun.core.domain.danmakuTimestampMs
import top.yukonga.miuix.kmp.window.WindowDialog
import com.aritxonly.bdfun.core.domain.DanmakuRuleKind
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.core.common.formatDuration
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DanmakuPoolRow(item: DanmakuItem, vote: DanmakuVote?, expanded: Boolean, onSelect: () -> Unit, action: (PoolAction) -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column {
        Row(Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(formatDuration((item.timeMs / 1000).toInt()), color = colors.onSurfaceVariantSummary)
            if (item.self) Text("我的", color = colors.primary)
            Text(item.text, color = colors.onBackground, modifier = Modifier.weight(1f))
            val likes = vote?.likes ?: item.likeCount
            if (likes > 0) Text("×$likes", color = colors.onSurfaceVariantSummary)
        }
        if (expanded) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            val actions = buildList {
                add(PoolAction.Seek)
                if (danmakuTimestampMs(item.text, Long.MAX_VALUE) != null) add(PoolAction.Timestamp)
                add(PoolAction.Copy)
                if (item.text.isNotBlank()) add(PoolAction.FollowSend)
                if (item.id > 0) {
                    add(PoolAction.Like)
                    if (!item.self) add(PoolAction.Report)
                }
                if (item.self && item.id > 0) add(PoolAction.Recall)
                if (!item.self && item.userHash.isNotBlank()) add(PoolAction.Block)
            }
            actions.forEach { option ->
                val liked = option == PoolAction.Like && vote?.liked == true
                Button(onClick = { action(option) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        DanmakuManagementGlyph(option, if (liked) colors.primary else colors.onBackground, liked)
                        Text(if (liked) "取消点赞" else option.title)
                    }
                }
            }
        }
    }
}

@Composable
internal fun DanmakuRulesPanel(
    rules: List<DanmakuCloudRule>, onBack: () -> Unit,
    add: suspend (DanmakuRuleKind, String) -> Boolean, remove: suspend (Long) -> Boolean,
    sync: suspend () -> Boolean,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    var kind by remember { mutableStateOf(DanmakuRuleKind.Keyword) }
    var value by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Button(onClick = onBack) { Text("返回") }
        DanmakuRuleKind.entries.forEach { option ->
            Button(onClick = { kind = option }) { Text(if (option == kind) "✓ ${option.label()}" else option.label()) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(value, { value = it }, modifier = Modifier.weight(1f).padding(8.dp), singleLine = true,
            textStyle = TextStyle(color = colors.onBackground), cursorBrush = SolidColor(colors.primary),
            decorationBox = { field ->
                Box {
                    if (value.isBlank()) Text(if (kind == DanmakuRuleKind.Sender) "发送者哈希" else "输入${kind.label()}",
                        color = colors.onSurfaceVariantSummary)
                    field()
                }
            })
        Button(enabled = value.isNotBlank() && !busy, onClick = {
            busy = true
            scope.launch {
                try { if (add(kind, value)) value = "" else context.showToast("添加屏蔽规则失败") }
                finally { busy = false }
            }
        }) { Text("添加") }
    }
    Button(enabled = !busy, onClick = {
        busy = true
        scope.launch {
            try { context.showToast(if (sync()) "弹幕设置已同步" else "同步失败，请确认已登录") }
            finally { busy = false }
        }
    }) { Text("上传当前弹幕设置") }
    LazyColumn {
        items(rules, key = { it.id }) { rule ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${rule.kind.label()} · ${rule.value}", color = colors.onBackground, modifier = Modifier.weight(1f))
                Button(onClick = { scope.launch { if (!remove(rule.id)) context.showToast("移除屏蔽规则失败") } }) { Text("移除") }
            }
        }
    }
}

private fun DanmakuRuleKind.label() = when (this) {
    DanmakuRuleKind.Keyword -> "关键词"
    DanmakuRuleKind.Pattern -> "正则"
    DanmakuRuleKind.Sender -> "作者"
}


@Composable
internal fun DanmakuPoolList(
    entries: List<DanmakuItem>, cid: Long, recalled: Set<Pair<Long, Long>>, selected: DanmakuItem?,
    votes: Map<Pair<Long, Long>, DanmakuVote>, openRules: () -> Unit,
    onSelect: (DanmakuItem) -> Unit, onAction: (DanmakuItem, PoolAction) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var byLikes by remember { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    val visible = remember(entries, cid, recalled, query, byLikes, votes) {
        val items = entries.filter { (cid to it.id) !in recalled && it.text.contains(query.trim(), ignoreCase = true) }
        if (byLikes) items.sortedByDescending { votes[cid to it.id]?.likes ?: it.likeCount }
        else items.sortedBy { it.timeMs }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(query, { query = it }, modifier = Modifier.weight(1f).padding(8.dp), singleLine = true,
            textStyle = TextStyle(color = colors.onBackground), cursorBrush = SolidColor(colors.primary),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) Text("搜索弹幕", color = colors.onSurfaceVariantSummary)
                    field()
                }
            })
        Button(onClick = { byLikes = !byLikes }) { Text(if (byLikes) "按点赞" else "按时间") }
        Button(onClick = openRules) { Text("屏蔽规则") }
    }
    Text("${visible.size} 条已加载弹幕", color = colors.onSurfaceVariantSummary, modifier = Modifier.padding(vertical = 8.dp))
    LazyColumn {
        items(visible) { item ->
            DanmakuPoolRow(item, votes[cid to item.id], selected == item, { onSelect(item) }) { onAction(item, it) }
        }
    }
}

@Composable
internal fun DanmakuReportDialog(
    item: DanmakuItem, onDismiss: () -> Unit,
    send: suspend (DanmakuReportReason, String) -> Boolean,
) {
    var reason by remember(item) { mutableStateOf<DanmakuReportReason?>(null) }
    var content by remember(item) { mutableStateOf("") }
    var busy by remember(item) { mutableStateOf(false) }
    val colors = MiuixTheme.colorScheme
    val scope = rememberCoroutineScope()
    WindowDialog(show = true, title = "举报弹幕", summary = item.text, onDismissRequest = { if (!busy) onDismiss() }) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DanmakuReportReason.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { option ->
                        Button(onClick = { reason = option }, modifier = Modifier.weight(1f)) {
                            Text(if (reason == option) "✓ ${option.title}" else option.title)
                        }
                    }
                }
            }
            if (reason == DanmakuReportReason.Other) BasicTextField(content, { content = it },
                modifier = Modifier.fillMaxWidth().padding(8.dp), textStyle = TextStyle(color = colors.onBackground),
                cursorBrush = SolidColor(colors.primary), decorationBox = { field ->
                    Box {
                        if (content.isBlank()) Text("请填写举报说明", color = colors.onSurfaceVariantSummary)
                        field()
                    }
                })
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) { Text("取消") }
                Button(enabled = !busy && reason != null && (reason != DanmakuReportReason.Other || content.isNotBlank()),
                    modifier = Modifier.weight(1f), onClick = {
                        val chosen = reason ?: return@Button
                        busy = true
                        scope.launch {
                            try { if (send(chosen, content.trim())) onDismiss() }
                            finally { busy = false }
                        }
                    }) { Text("提交举报") }
            }
        }
    }
}


@Composable
internal fun DanmakuFollowSendDialog(text: String?, onDismiss: () -> Unit, send: suspend (String) -> Boolean) {
    val content = text ?: return
    val scope = rememberCoroutineScope()
    var busy by remember(content) { mutableStateOf(false) }
    WindowDialog(show = true, title = "发送同款弹幕", summary = content, onDismissRequest = { if (!busy) onDismiss() }) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) { Text("取消") }
            Button(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                busy = true
                scope.launch {
                    try { if (send(content)) onDismiss() }
                    finally { busy = false }
                }
            }) { Text("发送") }
        }
    }
}
