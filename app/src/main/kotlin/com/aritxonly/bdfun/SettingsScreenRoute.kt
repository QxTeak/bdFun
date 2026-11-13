package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToOnboarding
import com.aritxonly.bdfun.navigateToLiveRoom
import com.aritxonly.bdfun.navigateToHelp
import com.aritxonly.bdfun.navigateToDonation
import com.aritxonly.bdfun.navigateToCredits
import com.aritxonly.bdfun.navigateToChangelog
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.ui.SettingsDependencies
import com.aritxonly.bdfun.ui.SettingsDestination
import com.aritxonly.bdfun.ui.SettingsNavigation
import com.aritxonly.bdfun.ui.SettingsScreen
import com.aritxonly.bdfun.ui.showToast

@Composable
internal fun SettingsScreenRoute(activity: ComponentActivity) {
    val dependencies = remember { SettingsDependencies(AppGraph::settingsCdnStateHolder, AppGraph.mineLayout) }
    SettingsScreen(AppGraph.settings,
        SettingsNavigation({ activity.finish() }, { AppGraph.logout(); activity.finish() },
            { destination -> openSettingsDestination(activity, destination) },
            { url -> openSettingsWebLink(activity, url) }, { activity.showToast(it) }),
        dependencies, AppGraph.api.isLoggedIn,
    )
}

private fun openSettingsDestination(context: ComponentActivity, destination: SettingsDestination) {
    when (destination) {
        SettingsDestination.Discussion -> openSettingsTelegramGroup(context)
        SettingsDestination.Changelog -> navigateToChangelog(context)
        SettingsDestination.Help -> navigateToHelp(context)
        SettingsDestination.Onboarding -> navigateToOnboarding(context)
        SettingsDestination.Donation -> navigateToDonation(context)
        SettingsDestination.Credits -> navigateToCredits(context)
        SettingsDestination.Project -> openSettingsWebLink(context, "https://github.com/QxTeak/bdFun")
        SettingsDestination.Logs -> openLogs(context)
        SettingsDestination.LivePreview -> navigateToLiveRoom(context)
    }
}

private fun openSettingsWebLink(context: ComponentActivity, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        .onFailure { context.showToast("无法打开链接") }
}

/** 与原入口相同：先尝试 Telegram；无客户端再交给系统。 */
private fun openSettingsTelegramGroup(context: ComponentActivity) {
    val uri = Uri.parse("https://t.me/+tQrDYf-hb702ZjM1")
    val telegram = Intent(Intent.ACTION_VIEW, uri).setPackage("org.telegram.messenger")
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    if (runCatching { context.startActivity(telegram) }.isSuccess) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure { context.showToast("无法打开链接") }
}
