package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.aritxonly.teak.common.settings.TeakSettingsActionRow
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsDropdownRow
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow

@Composable
internal fun SettingsVideoDetailCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "视频详情页") {
        SettingsVideoDetailCardRows1(runtime)
    }
}

@Composable
private fun SettingsVideoDetailCardRows1(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    TeakSettingsSwitchRow(
        title = "沉浸状态栏",
        checked = settings.videoImmersiveStatusBar,
        onCheckedChange = { settings.videoImmersiveStatusBar = it },
    )
    TeakSettingsSwitchRow(
        title = "显示评论 IP 属地",
        checked = settings.showIpLocation,
        onCheckedChange = { settings.showIpLocation = it },
    )
}

@Composable
internal fun SettingsDanmakuCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "弹幕") {
        SettingsDanmakuCardRows1(runtime)
    }
}

@Composable
private fun SettingsDanmakuCardRows1(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    TeakSettingsSwitchRow(
        title = "空降助手",
        checked = settings.sponsorSkipEnabled,
        onCheckedChange = { settings.sponsorSkipEnabled = it },
    )
    TeakSettingsSwitchRow(
        title = "弹幕计数",
        checked = settings.hotDanmakuEnabled,
        onCheckedChange = { settings.hotDanmakuEnabled = it },
    )
    TeakSettingsSwitchRow(
        title = "降低弹幕刷新率",
        checked = settings.danmakuLowRefresh,
        onCheckedChange = { settings.danmakuLowRefresh = it },
    )
}

@Composable
internal fun SettingsDynamicCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "动态页") {
        SettingsDynamicCardRows1(runtime)
    }
}

@Composable
private fun SettingsDynamicCardRows1(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    TeakSettingsSwitchRow(
        title = "动态视频自动播放",
        checked = settings.dynamicAutoPlay,
        onCheckedChange = { settings.dynamicAutoPlay = it },
    )
    TeakSettingsDropdownRow(
        title = "动态视频清晰度",
        options = listOf("1080P", "720P", "480P"),
        selected = listOf(80, 64, 32)
            .indexOf(settings.dynamicAutoPlayQuality)
            .coerceAtLeast(0),
        onSelected = {
            settings.dynamicAutoPlayQuality = listOf(80, 64, 32)[it]
        },
    )
}

@Composable
internal fun SettingsSearchCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "搜索页") {
        SettingsSearchCardRows1(runtime)
    }
}

@Composable
private fun SettingsSearchCardRows1(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    TeakSettingsSwitchRow(
        title = "显示热搜",
        checked = settings.showHotSearch,
        onCheckedChange = { settings.showHotSearch = it },
    )
}

@Composable
internal fun SettingsMineCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "我的页") {
        SettingsMineCardRows1(runtime)
        val layout by runtime.mineLayout.state.collectAsState()
        TeakSettingsSwitchRow(title = "展开历史记录", checked = layout.history,
            onCheckedChange = runtime.mineLayout::setHistory)
        TeakSettingsSwitchRow(title = "展开我的收藏", checked = layout.favorites,
            onCheckedChange = runtime.mineLayout::setFavorites)
        TeakSettingsSwitchRow(title = "展开稍后再看", checked = layout.watchLater,
            onCheckedChange = runtime.mineLayout::setWatchLater)
    }
}

@Composable
private fun SettingsMineCardRows1(runtime: SettingsPageRuntime) {
    val settings = runtime.settings
    TeakSettingsSwitchRow(
        title = "显示「我的评论与弹幕」入口",
        checked = settings.sentHistoryEntry,
        onCheckedChange = { settings.sentHistoryEntry = it },
    )
}

@Composable
internal fun SettingsAccountCard(runtime: SettingsPageRuntime) {
    TeakSettingsCard(title = "账号") {
        SettingsAccountCardRows1(runtime)
    }
}

@Composable
private fun SettingsAccountCardRows1(runtime: SettingsPageRuntime) {
    val onLogout = runtime.navigation.onLogout
    TeakSettingsActionRow(
        title = "退出登录",
        color = Color(0xFFE5484D),
        onClick = onLogout,
    )
}
