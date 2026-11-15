package com.aritxonly.bdfun.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import androidx.compose.runtime.collectAsState

@Composable
internal fun SettingsScreen(settings: SettingsBindings, navigation: SettingsNavigation, dependencies: SettingsDependencies, loggedIn: Boolean, indexed: com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder,
    extensions: Map<String, @Composable (com.aritxonly.bdfun.core.model.settings.Setting) -> Unit> = emptyMap()) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val cdn = remember(dependencies) { dependencies.createCdn() }
    val cdnState by cdn.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(cdn) { onDispose { cdn.close() } }
    LaunchedEffect(cdn) { cdn.load() }
    val pickWallpaper = rememberSettingsWallpaperPicker(settings)
    val runtime = SettingsPageRuntime(settings, cdn, cdnState, navigation, scope, pickWallpaper, dependencies.mineLayout, extensions)
    val browser = rememberSettingsBrowser()
    SettingsBrowser(indexed, runtime, loggedIn, browser)
}

@Composable
private fun rememberSettingsWallpaperPicker(settings: SettingsBindings): () -> Unit {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            settings.splashWallpaper = uri.toString()
        }
    }
    return { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
}
