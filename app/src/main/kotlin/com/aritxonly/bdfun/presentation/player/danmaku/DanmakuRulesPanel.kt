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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.domain.DanmakuCloudConfig
import com.aritxonly.bdfun.core.domain.DanmakuCloudRule
import com.aritxonly.teak.common.modal.MiuixBottomSheet
import com.aritxonly.bdfun.core.domain.DanmakuRuleKind

@Composable
internal fun DanmakuCloudRulesEntry(onInteract: () -> Unit) {
    val page = LocalVideoPageState.current
    val controller = page.danmakuModeration ?: return
    var open by remember(controller) { mutableStateOf(false) }
    DmActionRow(title = "云端屏蔽规则", onClick = { onInteract(); open = true })
    MiuixBottomSheet(show = open, title = "云端屏蔽规则", onDismissRequest = { open = false }) { padding ->
        val state by controller.state.collectAsState()
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            DanmakuRulesPanel(
                state.rules, onBack = { open = false },
                add = controller::addRule, remove = controller::deleteRule,
                sync = { controller.syncConfig(DanmakuCloudConfig.from(page.settings)) },
            )
        }
    }
}

@Composable
private fun DanmakuRulesPanel(
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
