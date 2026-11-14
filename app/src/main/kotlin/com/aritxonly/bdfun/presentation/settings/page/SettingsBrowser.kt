package com.aritxonly.bdfun.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BuildConfig
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import com.aritxonly.teak.common.search.teakSearchUnderlay
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsBrowser(controller: IndexedSettingsStateHolder, runtime: SettingsPageRuntime, loggedIn: Boolean, browser: SettingsBrowserStateHolder) {
    val navigation by browser.state.collectAsState()
    val settings by controller.state.collectAsState()
    val categories = visibleSettingsIndex(controller.index, settings.values, loggedIn, BuildConfig.DEBUG, runtime.extensions.keys)
    val configuration = LocalConfiguration.current
    val wide = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE && configuration.screenWidthDp >= 600
    val selected = categories.firstOrNull { it.route == navigation.route } ?: categories.firstOrNull().takeIf { wide }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    BackHandler { if (!browser.back()) runtime.navigation.onBack() }
    LaunchedEffect(navigation.searching) {
        if (!navigation.searching) { focus.clearFocus(); keyboard?.hide() }
    }
    LaunchedEffect(navigation.highlight) {
        navigation.highlight?.let { kotlinx.coroutines.delay(1800); browser.clearHighlight(it) }
    }
    val background = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop { drawRect(background); drawContent() }
    val session = SettingsBrowserSession(browser, navigation, selected, wide, categories)
    CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
            Box(Modifier.fillMaxSize().background(background)) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Row(Modifier.fillMaxSize()) {
                        if (wide || selected == null) {
                            val hideCategories = navigation.searching && (!wide || navigation.searchRoute == null)
                            SettingsCategoryList(session, Modifier.then(if (wide) Modifier.width(280.dp) else Modifier.fillMaxSize())
                                .settingsUnderlay(hideCategories, runtime.settings.animationsEnabled))
                        }
                        if (selected != null) {
                            SettingsCategoryContent(session, settings.values, runtime, controller,
                                Modifier.weight(1f).fillMaxHeight().settingsUnderlay(navigation.searching, runtime.settings.animationsEnabled))
                        }
                    }
                    if (navigation.searching) {
                        val hits = searchSettingsCategories(categories, navigation.query, navigation.searchRoute)
                        SettingsSearchResults(session, hits, Modifier.fillMaxSize()
                            .padding(start = if (wide) 280.dp else 0.dp))
                    }
                }
                if (selected == null || wide || navigation.searching) SettingsSearchDock(session)
            }
        }
    }
}

@Composable
private fun Modifier.settingsUnderlay(hidden: Boolean, animationsEnabled: Boolean): Modifier {
    val progress by animateFloatAsState(if (hidden) 1f else 0f,
        if (animationsEnabled) TeakSearchTransitionDefaults.spec() else tween(0), label = "settingsSearchMotion")
    val alpha by animateFloatAsState(if (hidden) 0f else 1f,
        tween(if (animationsEnabled) TeakSearchTransitionDefaults.UnderlayAlphaMs else 0), label = "settingsSearchAlpha")
    return this.blockTouches(hidden).then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier)
        .teakSearchUnderlay(progress, alpha)
}

@Composable
internal fun rememberSettingsBrowser(): SettingsBrowserStateHolder {
    val saver = remember {
        Saver<SettingsBrowserStateHolder, List<String>>(save = {
            val s = it.state.value
            listOf(s.route.orEmpty(), s.searching.toString(), s.searchRoute.orEmpty(), s.query)
        }, restore = {
            SettingsBrowserStateHolder(SettingsBrowserState(it[0].ifEmpty { null }, it[1].toBoolean(),
                it[2].ifEmpty { null }, it[3]))
        })
    }
    return rememberSaveable(saver = saver) { SettingsBrowserStateHolder() }
}
