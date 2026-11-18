package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.model.home.HomeChannel
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.settings.TeakSettingsSwitchRow
import com.aritxonly.teak.common.settings.TeakSettingsValueRow

@Composable
internal fun HomeChannelsSettingsPage(settings: SettingsBindings) {
    val list = rememberLazyListState()
    TrackTeakPageScroll(list)
    LazyColumn(Modifier.fillMaxSize(), state = list, contentPadding = settingsListPadding()) {
        item { TeakLargeTitle("首页频道") }
        item {
            TeakSettingsCard {
                TeakSettingsValueRow("推荐", "始终开启")
                HomeChannel.entries.drop(1).forEach { channel ->
                    TeakSettingsSwitchRow(
                        title = channel.title,
                        description = if (channel == HomeChannel.Popular || channel == HomeChannel.Cinema)
                            "内容即将上线" else "",
                        checked = channel.key in settings.homeChannels,
                        onCheckedChange = { enabled ->
                            settings.homeChannels = if (enabled) settings.homeChannels + channel.key
                                else settings.homeChannels - channel.key
                        },
                    )
                }
            }
        }
    }
}
