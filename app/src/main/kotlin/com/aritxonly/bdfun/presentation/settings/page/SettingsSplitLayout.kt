package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.SettingValue
import com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.SettingsSplitPolicy

@Composable
internal fun SettingsSplitLayout(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder) {
    TeakBareTheme(runtime.settings) { _, _ ->
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val total = maxWidth.value
            val left = SettingsSplitPolicy.leftWidth(total)
            Row(Modifier.fillMaxSize()) {
                SettingsBrowserPane(session, values, runtime, controller, SettingsPane.Categories,
                    Modifier.width(left.dp).fillMaxHeight().clipToBounds())
                SettingsPaneNavigation(session, values, runtime, controller, SettingsPane.Details,
                    Modifier.weight(1f).fillMaxHeight().clipToBounds())
            }
        }
    }
}
