package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.navigation.createRouteIntent

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
