package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

internal val settingsEmbeddedDestinations = setOf(SettingsDestination.Help, SettingsDestination.Changelog,
    SettingsDestination.Credits, SettingsDestination.Donation)

internal fun settingsDetailTitle(destination: SettingsDestination): String = when (destination) {
    SettingsDestination.Help -> "帮助中心"
    SettingsDestination.Changelog -> "更新日志"
    SettingsDestination.Credits -> "引用"
    SettingsDestination.Donation -> "捐赠"
    else -> error("不是内嵌设置页面")
}

@Composable
internal fun SettingsDetailContent(destination: SettingsDestination, onBack: () -> Unit) {
    when (destination) {
        SettingsDestination.Help -> HelpScreen()
        SettingsDestination.Changelog -> ChangelogScreen(onBack)
        SettingsDestination.Credits -> CreditsScreen()
        SettingsDestination.Donation -> DonationScreen()
        else -> error("不是内嵌设置页面")
    }
}
