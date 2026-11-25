package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val layout = remember(configuration.orientation, configuration.screenWidthDp, configuration.screenHeightDp) {
        danmakuReportLayout(
            landscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE,
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
        )
    }
    WindowDialog(
        show = true,
        title = "举报弹幕",
        summary = item.text,
        onDismissRequest = { if (!busy) onDismiss() },
        insideMargin = androidx.compose.ui.unit.DpSize(16.dp, if (layout.compactButtons) 12.dp else 16.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DanmakuReportReasonList(
                selected = reason,
                onSelect = { reason = it },
                layout = layout,
                otherVisible = reason == DanmakuReportReason.Other,
                content = content,
                onContentChange = { content = it },
            )
            DanmakuReportDialogActions(
                cancelEnabled = !busy,
                submitEnabled = !busy && reason != null &&
                    (reason != DanmakuReportReason.Other || content.isNotBlank()),
                onCancel = onDismiss,
                onSubmit = {
                    val chosen = reason ?: return@DanmakuReportDialogActions
                    busy = true
                    scope.launch {
                        try { if (send(chosen, content.trim())) onDismiss() }
                        finally { busy = false }
                    }
                },
            )
        }
    }
}

@Composable
private fun DanmakuReportReasonList(
    selected: DanmakuReportReason?,
    onSelect: (DanmakuReportReason) -> Unit,
    layout: DanmakuReportLayout,
    otherVisible: Boolean,
    content: String,
    onContentChange: (String) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        Modifier.fillMaxWidth()
            .then(layout.maxContentHeightDp?.let { Modifier.heightIn(max = it.dp) } ?: Modifier)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(if (layout.compactButtons) 4.dp else 6.dp),
    ) {
        DanmakuReportReason.entries.chunked(layout.columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { option ->
                    Button(
                        onClick = { onSelect(option) },
                        modifier = Modifier.weight(1f).heightIn(min = if (layout.compactButtons) 35.dp else 42.dp),
                    ) {
                        Row(
                            modifier = if (layout.compactButtons) {
                                Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
                            } else {
                                Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            },
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                if (selected == option) "✓ ${option.title}" else option.title,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = if (layout.compactButtons) 12.sp else 13.sp,
                            )
                        }
                    }
                }
                repeat(layout.columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        if (otherVisible) {
            BasicTextField(
                content,
                onContentChange,
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                textStyle = TextStyle(color = colors.onBackground),
                cursorBrush = SolidColor(colors.primary),
                decorationBox = { field ->
                    Box {
                        if (content.isBlank()) Text("请填写举报说明", color = colors.onSurfaceVariantSummary)
                        field()
                    }
                },
            )
        }
    }
}

@Composable
private fun DanmakuReportDialogActions(
    cancelEnabled: Boolean,
    submitEnabled: Boolean,
    onCancel: () -> Unit,
    onSubmit: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Button(onClick = onCancel, enabled = cancelEnabled, modifier = Modifier.weight(1f)) { Text("取消") }
        Button(enabled = submitEnabled, modifier = Modifier.weight(1f), onClick = onSubmit) { Text("提交举报") }
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
