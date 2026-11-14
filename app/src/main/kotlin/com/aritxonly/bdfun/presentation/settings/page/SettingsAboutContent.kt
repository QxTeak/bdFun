package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.SettingValue
import com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.TeakLargeTitle

@Composable
internal fun SettingsAboutContent(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, modifier: Modifier) {
    val category = session.selected ?: return
    val scroll = rememberScrollState()
    if (!session.navigation.searching) TrackTeakPageScroll(scroll)
    val distance = with(LocalDensity.current) { 320.dp.toPx() }
    val progress by remember(scroll, distance) { derivedStateOf { (scroll.value / distance).coerceIn(0f, 1f) } }
    Box(modifier) {
        SettingsAboutFloatingBackground(Modifier.matchParentSize(), { 1f - progress }, runtime.settings.animationsEnabled)
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(settingsListPadding())) {
            TeakLargeTitle(category.name)
            category.groups.forEach { group ->
                SettingsIndexedGroup(group, session, values, runtime, controller, progress)
            }
        }
    }
}
