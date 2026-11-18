package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.model.message.ImSetting
import com.aritxonly.bdfun.feature.message.application.MessageTool
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageNotificationDropdown(type: Int) {
    val owner = AppGraph.session.identity
    val controller = remember(owner, type) { AppGraph.messageToolsStateHolder(MessageTool.Settings(type)) }
    val scope = rememberMessageTaskScope(controller, controller::close)
    val state by controller.state.collectAsState()
    val context = LocalContext.current
    MessageOverflowMenu { expanded, _ ->
        LaunchedEffect(controller, expanded) { if (expanded) controller.load() }
        val available = state.settings.filter { it.choices.isNotEmpty() || it.enabled != null }
        when {
            state.error != null -> GlassPopupItem(
                text = "${state.error} · 重试",
                onClick = { scope.launch { controller.load() } },
            )
            available.isEmpty() -> GlassPopupItem(
                text = if (state.loading) "加载中…" else "暂无可选设置",
                enabled = false,
                onClick = {},
            )
        }
        available.forEach { setting ->
            NotificationChoices(setting, enabled = !state.loading && !state.busy) { value, choice ->
                scope.launch { controller.setting(setting, value, choice)?.let(context::showToast) }
            }
        }
    }
}

@Composable
private fun NotificationChoices(setting: ImSetting, enabled: Boolean, onSet: (Boolean?, Int?) -> Unit) {
    if (setting.title.isNotBlank()) {
        Text(
            text = setting.title,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
            fontSize = 12.sp,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
        )
    }
    for (choice in setting.choices) {
        GlassPopupItem(
            text = choice.text,
            selected = choice.selected,
            enabled = enabled,
            onClick = { if (!choice.selected) onSet(null, choice.value) },
        )
    }
    val current = setting.enabled ?: return
    for ((label, value) in listOf("开启" to true, "关闭" to false)) {
        GlassPopupItem(
            text = label,
            selected = current == value,
            enabled = enabled,
            onClick = { if (current != value) onSet(value, null) },
        )
    }
}
