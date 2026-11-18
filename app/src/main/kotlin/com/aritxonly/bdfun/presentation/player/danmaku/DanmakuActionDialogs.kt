package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.core.domain.DanmakuReportReason
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import top.yukonga.miuix.kmp.window.WindowDialog

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
