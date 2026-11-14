package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.*
import com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.SettingUpdate
import com.aritxonly.teak.common.settings.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun IndexedSettingComponent(entry: Setting, value: SettingValue?, runtime: SettingsPageRuntime,
    controller: IndexedSettingsStateHolder) {
    when (entry.type) {
        "switch", "boolean" -> {
            val checked = (value as? SettingValue.Flag)?.value ?: return
            TeakSettingsSwitchRow(entry.title, entry.subtitle.orEmpty(), checked xor entry.inverted) {
                updateIndexedSetting(controller, runtime, entry.prefName, SettingValue.Flag(it xor entry.inverted))
            }
        }
        "dropdown", "select" -> IndexedDropdown(entry, value ?: entry.default, runtime, controller)
        "cdn_dropdown" -> CdnIndexedDropdown(entry, runtime)
        "font_scale" -> GlobalFontScaleRow(runtime.settings.fontScale, { runtime.settings.fontScale = it }, entry.title)
        "extension" -> runtime.extensions[entry.prefName]?.invoke(entry)
        "app_identity" -> SettingsAboutAppHeader(entry.title)
        "page_switches" -> ScrollPageSwitches(runtime)
        "mine_switch" -> MineIndexedSwitch(entry, runtime)
        else -> IndexedActionComponent(entry, runtime)
    }
}

@Composable
private fun IndexedDropdown(entry: Setting, value: SettingValue, runtime: SettingsPageRuntime,
    controller: IndexedSettingsStateHolder) {
    val ui = remember(entry.prefName) { SettingsAppearancePresentationStateHolder() }
    LaunchedEffect(value) { ui.pendingDark = null }
    val description = if (entry.prefName == "decoder_mode") when ((value as? SettingValue.Number)?.value) {
        1 -> "优先硬件解码，失败时自动回退"
        2 -> "只使用软件解码，兼容性优先"
        3 -> "只使用硬件解码，失败时不回退"
        else -> "自动选择，优先硬件解码"
    } else entry.subtitle.orEmpty()
    TeakSettingsDropdownRow(title = entry.title, description = description,
        options = entry.options.map { it.title },
        selected = ui.pendingDark ?: entry.options.indexOfFirst { it.value == value }.coerceAtLeast(0),
        onSelected = { index ->
            if (entry.prefName in setOf("dark_mode", "color_style")) {
                ui.pendingDark = index
                runtime.scope.launch {
                    delay(260)
                    updateIndexedSetting(controller, runtime, entry.prefName, entry.options[index].value)
                }
            } else updateIndexedSetting(controller, runtime, entry.prefName, entry.options[index].value)
        },
    )
}

@Composable
private fun CdnIndexedDropdown(entry: Setting, runtime: SettingsPageRuntime) {
    TeakSettingsDropdownRow(title = entry.title, description = entry.subtitle.orEmpty(),
        options = runtime.cdnState.labels,
        closedValue = runtime.cdn.services[runtime.cdn.selectedIndex(runtime.settings.cdnService)].label,
        selected = runtime.cdn.selectedIndex(runtime.settings.cdnService), panelWidth = 300.dp,
        onSelected = { runtime.settings.cdnService = runtime.cdn.services[it].id },
    )
}

@Composable
private fun ScrollPageSwitches(runtime: SettingsPageRuntime) {
    scrollTopPageEntries.forEach { (key, label) ->
        TeakSettingsSwitchRow(label, checked = runtime.settings.isScrollTopPageEnabled(key),
            onCheckedChange = { runtime.settings.setScrollTopPageEnabled(key, it) })
    }
}

@Composable
private fun MineIndexedSwitch(entry: Setting, runtime: SettingsPageRuntime) {
    val layout by runtime.mineLayout.state.collectAsState()
    val checked = when (entry.prefName) {
        "history" -> layout.history
        "favorites" -> layout.favorites
        else -> layout.watchLater
    }
    TeakSettingsSwitchRow(entry.title, entry.subtitle.orEmpty(), checked) { value ->
        when (entry.prefName) {
            "history" -> runtime.mineLayout.setHistory(value)
            "favorites" -> runtime.mineLayout.setFavorites(value)
            "watch_later" -> runtime.mineLayout.setWatchLater(value)
        }
    }
}

private fun updateIndexedSetting(controller: IndexedSettingsStateHolder, runtime: SettingsPageRuntime,
    key: String, value: SettingValue) {
    val result = controller.update(key, value)
    if (result is SettingUpdate.Rejected) runtime.navigation.onNotify(result.reason)
    if (key == "interface_style") com.aritxonly.teak.common.theme.TeakUiStyle.style = runtime.settings.interfaceStyle
}
