package com.aritxonly.bdfun.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.teak.common.settings.TeakSettingsPage
import androidx.compose.runtime.collectAsState

@Composable
internal fun SettingsScreen(settings: SettingsBindings, navigation: SettingsNavigation, dependencies: SettingsDependencies, loggedIn: Boolean) {
    BackHandler(onBack = navigation.onBack)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val cdn = remember(dependencies) { dependencies.createCdn() }
    val cdnState by cdn.state.collectAsState()
    androidx.compose.runtime.DisposableEffect(cdn) { onDispose { cdn.close() } }
    LaunchedEffect(cdn) { cdn.load() }
    val pickWallpaper = rememberSettingsWallpaperPicker(settings)
    val runtime = SettingsPageRuntime(settings, cdn, cdnState, navigation, scope, pickWallpaper, dependencies.mineLayout)
    TeakSettingsPage(title = "设置") {
        item(key = "appearance") { SettingsAppearanceCard(runtime) }
        item(key = "player") { SettingsPlayerCard(runtime) }
        item(key = "video_detail") { SettingsVideoDetailCard(runtime) }
        item(key = "danmaku") { SettingsDanmakuCard(runtime) }
        item(key = "dynamics") { SettingsDynamicCard(runtime) }
        item(key = "search") { SettingsSearchCard(runtime) }
        item(key = "mine") { SettingsMineCard(runtime) }
        item(key = "developers") { SettingsDevelopersCard(runtime) }
        item(key = "about") { SettingsAboutCard(runtime) }
        if (loggedIn) item(key = "account") { SettingsAccountCard(runtime) }
    }
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
