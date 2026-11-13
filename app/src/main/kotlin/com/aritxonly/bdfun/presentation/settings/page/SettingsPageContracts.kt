package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.feature.settings.application.CdnSettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.CdnSettingsUiState

internal enum class SettingsDestination { Discussion, Changelog, Help, Onboarding, Donation, Credits, Project, Logs, LivePreview }
internal class SettingsNavigation(
    val onBack: () -> Unit, val onLogout: () -> Unit,
    val onAbout: (SettingsDestination) -> Unit, val onDeveloper: (String) -> Unit, val onNotify: (String) -> Unit,
)
internal class SettingsDependencies(val createCdn: () -> CdnSettingsStateHolder,
    val mineLayout: com.aritxonly.bdfun.core.application.MineLayoutStateHolder)
internal class SettingsPageRuntime(
    val settings: SettingsBindings, val cdn: CdnSettingsStateHolder, val cdnState: CdnSettingsUiState,
    val navigation: SettingsNavigation, val scope: kotlinx.coroutines.CoroutineScope, val onPickWallpaper: () -> Unit,
    val mineLayout: com.aritxonly.bdfun.core.application.MineLayoutStateHolder,
)
