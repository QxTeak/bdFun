package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
        "Background" -> MiuixIcons.Normal.Background
        "Play" -> MiuixIcons.Normal.Play
        "Messages" -> MiuixIcons.Normal.Messages
        "Home" -> MiuixIcons.Normal.Home
        "ContactsCircle" -> MiuixIcons.Normal.ContactsCircle
        "Info" -> MiuixIcons.Normal.Info
        else -> MiuixIcons.Normal.Settings
    }
    Box(Modifier.padding(end = 16.dp).size(32.dp)
        .background(Color(0xFF000000 or entry.color.removePrefix("#").toLong(16)), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
    }
}
