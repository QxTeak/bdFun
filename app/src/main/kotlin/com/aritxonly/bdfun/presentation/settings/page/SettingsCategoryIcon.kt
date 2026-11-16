package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aritxonly.teak.common.icons.AppGlyph
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.SettingsEntry
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.*

@Composable
internal fun SettingsCategoryIcon(entry: SettingsEntry) {
    val icon = when (entry.icon) {
        "Background" -> MiuixIcons.Background
        "Play" -> MiuixIcons.Play
        "Messages" -> MiuixIcons.Messages
        "Home" -> MiuixIcons.Home
        "ContactsCircle" -> MiuixIcons.ContactsBook
        "Info" -> MiuixIcons.Info
        else -> MiuixIcons.Settings
    }
    Box(Modifier.padding(end = 16.dp).size(32.dp)
        .background(Color(0xFF000000 or entry.color.removePrefix("#").toLong(16)), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center) {
        AppGlyph(icon, contentDescription = null, tint = Color.White, size = 23.dp)
    }
}
