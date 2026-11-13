package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.R
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsValueRow

@Composable
internal fun SettingsDevelopersCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "开发者") {
        SettingsDevelopersCardRows1(runtime)
    }
}

@Composable
private fun SettingsDevelopersCardRows1(runtime: SettingsPageRuntime) {
    DeveloperRow(
        name = "AritxOnly",
        avatar = R.drawable.dev_artix,
        url = "https://github.com/AritxOnly",
        onOpen = runtime.navigation.onDeveloper,
    )
    DeveloperRow(
        name = "Teak",
        avatar = R.drawable.dev_teak,
        url = "https://github.com/QxTeak",
        onOpen = runtime.navigation.onDeveloper,
    )
}

@Composable
internal fun SettingsAboutCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "关于") {
        SettingsAboutCardRows1(runtime)
    }
}

@Composable
private fun SettingsAboutCardRows1(runtime: SettingsPageRuntime) {
    TeakSettingsValueRow(
        title = "版本",
        value = BuildConfig.VERSION_NAME,
    )
    TeakSettingsActionRow(title = "加入讨论组", trailing = "Telegram 群") {
        runtime.navigation.onAbout(SettingsDestination.Discussion)
    }
    TeakSettingsActionRow(title = "更新日志") {
        runtime.navigation.onAbout(SettingsDestination.Changelog)
    }
    TeakSettingsActionRow(title = "帮助中心") {
        runtime.navigation.onAbout(SettingsDestination.Help)
    }
    TeakSettingsActionRow(title = "查看开始页") {
        runtime.navigation.onAbout(SettingsDestination.Onboarding)
    }
    TeakSettingsActionRow(title = "捐赠") {
        runtime.navigation.onAbout(SettingsDestination.Donation)
    }
    TeakSettingsActionRow(title = "引用") {
        runtime.navigation.onAbout(SettingsDestination.Credits)
    }
    TeakSettingsActionRow(title = "项目地址") {
        runtime.navigation.onAbout(SettingsDestination.Project)
    }
    if (BuildConfig.DEBUG) {
        TeakSettingsActionRow(title = "直播间（预览）", trailing = "调试") {
            runtime.navigation.onAbout(SettingsDestination.LivePreview)
        }
        TeakSettingsActionRow(title = "日志抓取", trailing = "调试") {
            runtime.navigation.onAbout(SettingsDestination.Logs)
        }
    }
}
