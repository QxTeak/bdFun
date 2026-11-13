package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import com.aritxonly.bdfun.navigateToMessageFeed
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.icon.extended.Contacts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageShortcutGroup(unread: Map<String, Int>, events: MessageScreenEvents) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp)
            .clip(com.aritxonly.teak.common.chrome.TeakContinuousCornerShape(22.dp))
            .background(colors.surfaceContainer)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        MessageShortcut(
            label = "回复我的",
            modifier = Modifier.weight(1f),
            onClick = events.onOpenReplies,
            unread = unread["reply"] ?: 0,
        ) { androidx.compose.material3.Icon(MiuixIcons.Messages, null, Modifier.size(ShortcutGlyphSize), tint = MessageGreen) }
        MessageShortcut(
            label = "收到喜欢",
            modifier = Modifier.weight(1f),
            onClick = events.onOpenLikes,
            unread = unread["like"] ?: 0,
        ) { ResourceGlyph(res = TeakIcon.Like, tint = MessagePink, size = ShortcutGlyphSize) }
        MessageShortcut(
            label = "@我",
            modifier = Modifier.weight(1f),
            onClick = { navigateToMessageFeed(context, "at") },
            unread = unread["at"] ?: 0,
        ) { androidx.compose.material3.Icon(MiuixIcons.Contacts, null, Modifier.size(ShortcutGlyphSize), tint = MessageBlue) }
        MessageShortcut(label = "系统通知", modifier = Modifier.weight(1f),
            unread = unread["sys_msg"] ?: 0,
            onClick = { navigateToMessageFeed(context, "system") }) {
            androidx.compose.material3.Icon(MiuixIcons.Messages, null, Modifier.size(ShortcutGlyphSize), tint = colors.primary)
        }
    }
}
