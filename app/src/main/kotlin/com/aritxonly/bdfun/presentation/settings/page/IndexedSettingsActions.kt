package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.R
import com.aritxonly.bdfun.core.model.settings.Setting
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsValueRow

/** 索引仅选择已注册动作，不能注入 Intent、URL 或执行代码。 */
@Composable
internal fun IndexedActionComponent(entry: Setting, runtime: SettingsPageRuntime) {
    when (entry.prefName) {
        "developer_aritx" -> DeveloperRow(entry.title, R.drawable.dev_artix, "https://github.com/AritxOnly", runtime.navigation.onDeveloper)
        "developer_teak" -> DeveloperRow(entry.title, R.drawable.dev_teak, "https://github.com/QxTeak", runtime.navigation.onDeveloper)
        "version" -> TeakSettingsValueRow(entry.title, BuildConfig.VERSION_NAME)
        "wallpaper_pick" -> TeakSettingsActionRow(entry.title,
            trailing = if (runtime.settings.splashWallpaper.isBlank()) "未设置" else "已设置",
            onClick = runtime.onPickWallpaper)
        "wallpaper_clear" -> TeakSettingsActionRow(entry.title) {
            runtime.settings.splashWallpaper = ""
            runtime.navigation.onNotify("已清除开屏壁纸")
        }
        "logout" -> TeakSettingsActionRow(entry.title, color = Color(0xFFE5484D), onClick = runtime.navigation.onLogout)
        else -> SettingsDestinationAction(entry, runtime)
    }
}

@Composable
private fun SettingsDestinationAction(entry: Setting, runtime: SettingsPageRuntime) {
    val destination = when (entry.prefName) {
        "discussion" -> SettingsDestination.Discussion
        "changelog" -> SettingsDestination.Changelog
        "help" -> SettingsDestination.Help
        "onboarding" -> SettingsDestination.Onboarding
        "donation" -> SettingsDestination.Donation
        "credits" -> SettingsDestination.Credits
        "project" -> SettingsDestination.Project
        "live_preview" -> SettingsDestination.LivePreview
        "logs" -> SettingsDestination.Logs
        else -> return
    }
    TeakSettingsActionRow(entry.title, trailing = when (entry.prefName) {
        "discussion" -> "Telegram 群"
        "live_preview", "logs" -> "调试"
        else -> ""
    }) { runtime.navigation.onAbout(destination) }
}
