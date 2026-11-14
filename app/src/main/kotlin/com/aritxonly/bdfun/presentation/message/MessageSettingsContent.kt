package com.aritxonly.bdfun.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.message.ImSetting
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.navigation.createRouteIntent
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageSettingRow(setting: ImSetting, busy: Boolean, onSet: (Boolean?, Int?) -> Unit) {
    val navigate = messageRouteAction()
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val pageType = setting.pageType
    val enabled = setting.enabled
    val open = {
        when {
            pageType != null -> navigate(Route.MessageTools("settings", type = pageType))
            setting.title == "黑名单" -> navigate(Route.MessageTools("blacklist"))
            setting.title == "消息屏蔽词" -> navigate(Route.MessageTools("keywords"))
            setting.url.startsWith("http") -> context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(setting.url)))
            setting.url.isNotBlank() -> context.showToast(setting.url)
        }
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth().clickable(enabled = !busy && (enabled != null || setting.choices.isEmpty())) {
            if (enabled != null) onSet(!enabled, null) else open()
        }.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                if (setting.title.isNotBlank()) Text(setting.title, color = colors.onBackground, fontSize = 15.sp)
                if (setting.subtitle.isNotBlank()) Text(setting.subtitle, color = colors.onBackgroundVariant, fontSize = 12.sp)
                if (setting.summary.isNotBlank()) Text(setting.summary, color = colors.onBackgroundVariant, fontSize = 12.sp)
            }
            if (enabled != null) top.yukonga.miuix.kmp.basic.Switch(checked = enabled,
                onCheckedChange = { onSet(it, null) }, enabled = !busy)
        }
        setting.choices.forEach { choice ->
            Text((if (choice.selected) "✓  " else "    ") + choice.text,
                color = if (choice.selected) colors.primary else colors.onBackground,
                modifier = Modifier.fillMaxWidth().clickable(enabled = !busy && !choice.selected) { onSet(null, choice.value) }
                    .padding(horizontal = 24.dp, vertical = 14.dp))
        }
    }
}
