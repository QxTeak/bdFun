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
    SettingsCategoryIcon(iconName = entry.icon, color = entry.color)
}

/**
 * 设置首页的分类图标：Miuix 图标 + 30dp 色块，颜色来自设置分组清单。
 *
 * 「捐款」这类不在清单里的入口也要用同一个组件，才能跟其它分类图标保持同一套观感。
 */
@Composable
internal fun SettingsCategoryIcon(iconName: String, color: String) {
    val icon = when (iconName) {
        "Background" -> MiuixIcons.Background
        "Play" -> MiuixIcons.Play
        "Messages" -> MiuixIcons.Messages
        "Home" -> MiuixIcons.Home
        "ContactsCircle" -> MiuixIcons.ContactsBook
        "Info" -> MiuixIcons.Info
        "BankCards" -> MiuixIcons.BankCards
        else -> MiuixIcons.Settings
    }
    Box(Modifier.padding(end = 16.dp).size(32.dp)
        .background(Color(0xFF000000 or color.removePrefix("#").toLong(16)), RoundedCornerShape(9.dp)),
        contentAlignment = Alignment.Center) {
        AppGlyph(icon, contentDescription = null, tint = Color.White, size = 23.dp)
    }
}
