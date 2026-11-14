package com.aritxonly.bdfun.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun SettingsBrowser(controller: IndexedSettingsStateHolder, runtime: SettingsPageRuntime, loggedIn: Boolean, browser: SettingsBrowserStateHolder) {
    val navigation by browser.state.collectAsState()
    val settings by controller.state.collectAsState()
    val visible = visibleSettingsIndex(controller.index, settings.values, loggedIn, BuildConfig.DEBUG, runtime.extensions.keys)
    val categories = visible.filter { it.layout != "root_footer" }
    val footers = visible.filter { it.layout == "root_footer" }
    val configuration = LocalConfiguration.current
    val wide = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp >= 600
    val selected = categories.firstOrNull { it.route == navigation.route } ?: categories.firstOrNull().takeIf { wide }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler { if (!browser.back()) runtime.navigation.onBack() }
    LaunchedEffect(navigation.searching) { if (!navigation.searching) { focus.clearFocus(); keyboard?.hide() } }
    LaunchedEffect(navigation.highlight) {
        navigation.highlight?.let { kotlinx.coroutines.delay(1800); browser.clearHighlight(it) }
    }
    val session = SettingsBrowserSession(browser, navigation, selected, wide, categories, footers)
    if (wide) SettingsSplitLayout(session, settings.values, runtime, controller)
    else SettingsBrowserPane(session, settings.values, runtime, controller, SettingsPane.Single, Modifier.fillMaxSize())
}

@Composable
internal fun rememberSettingsBrowser(): SettingsBrowserStateHolder {
    val saver = remember {
        Saver<SettingsBrowserStateHolder, List<String>>(save = {
            val s = it.state.value
            listOf(s.route.orEmpty(), s.searching.toString(), s.searchRoute.orEmpty(), s.query, s.splitFraction.toString())
        }, restore = {
            SettingsBrowserStateHolder(SettingsBrowserState(it[0].ifEmpty { null }, it[1].toBoolean(),
                it[2].ifEmpty { null }, it[3], splitFraction = it.getOrNull(4)?.toFloatOrNull() ?: SettingsSplitPolicy.DefaultFraction))
        })
    }
    return rememberSaveable(saver = saver) { SettingsBrowserStateHolder() }
}
