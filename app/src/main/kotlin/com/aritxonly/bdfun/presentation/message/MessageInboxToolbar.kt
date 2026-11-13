package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.feature.message.application.MessageSessionsUiState
import com.aritxonly.bdfun.navigation.createRouteIntent
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageInboxToolbar(snapshot: MessageSessionsUiState, menu: MessageSessionMenuStateHolder) {
    val context = LocalContext.current
    val navigate = messageRouteAction()
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Text("私信", color = MiuixTheme.colorScheme.onBackgroundVariant, modifier = Modifier.weight(1f))
        snapshot.outside.forEach { item ->
            Text(item.title, color = MiuixTheme.colorScheme.primary, modifier = Modifier.clickable {
                messageMenuAction(context, item, menu, navigate)
            }.padding(12.dp))
        }
        if (snapshot.outside.none { it.type == 8 }) Text("联系人", color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.clickable { navigate(Route.MessageTools("contacts")) }.padding(12.dp))
    }
}

internal fun messageMenuAction(context: Context, item: MessageMenuItem, menu: MessageSessionMenuStateHolder,
    navigate: (Route) -> Unit = { context.startActivity(createRouteIntent(context, it)) }) {
    menu.showMore = false
    when (item.type) {
        1 -> menu.confirmRead = true
        9 -> menu.confirmClear = true
        2 -> navigate(Route.MessageTools("settings"))
        8 -> navigate(Route.MessageTools("contacts"))
        4 -> {
            val talker = Regex("/(\\d{3,})").find(item.url)?.groupValues?.get(1)?.toLongOrNull()
            if (talker != null) navigate(MessageSession("1_$talker", talker, 1, item.title, item.icon, "", 0, 0, false).chatRoute())
        }
        else -> if (item.url.startsWith("http")) context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url)))
            else context.showToast("暂不支持此入口：${item.title}")
    }
}
