package com.aritxonly.bdfun.ui

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.IntSize
import com.aritxonly.bdfun.core.model.settings.SettingValue
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.chrome.TeakPageScaffold
import com.aritxonly.teak.common.chrome.rememberTeakPageScrollState
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import com.aritxonly.teak.common.search.teakSearchUnderlay
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal enum class SettingsPane { Single, Categories, Details }

@Composable
internal fun SettingsBrowserPane(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, pane: SettingsPane, modifier: Modifier) {
    BoxWithConstraints(modifier.clipToBounds()) {
        val parent = LocalWindowInfo.current
        val size = IntSize(constraints.maxWidth, constraints.maxHeight)
        val window = remember(parent, size) { object : WindowInfo by parent { override val containerSize = size } }
        val original = LocalConfiguration.current
        val configuration = remember(original, maxWidth, maxHeight) {
            Configuration(original).apply {
                screenWidthDp = maxWidth.value.toInt()
                screenHeightDp = maxHeight.value.toInt()
                smallestScreenWidthDp = minOf(screenWidthDp, screenHeightDp)
                orientation = if (screenWidthDp > screenHeightDp) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
            }
        }
        val searching = session.navigation.searching && (pane == SettingsPane.Single ||
            (session.navigation.searchRoute == null) == (pane == SettingsPane.Categories))
        val local = session.copy(navigation = session.navigation.copy(searching = searching))
        val categories = pane == SettingsPane.Categories || pane == SettingsPane.Single && session.selected == null
        val title = if (searching) "搜索设置" else if (categories) "设置" else session.selected?.name ?: "设置"
        val back = {
            if (pane == SettingsPane.Categories) {
                if (searching) session.browser.closeSearch() else runtime.navigation.onBack()
            } else if (!session.browser.back()) runtime.navigation.onBack()
        }
        CompositionLocalProvider(LocalWindowInfo provides window, LocalConfiguration provides configuration) {
            SettingsPaneScaffold(runtime, pane, title, back,
                search = if (!categories && !searching && session.selected != null) ({ session.browser.search(session.selected.route) }) else null,
            ) { SettingsPaneBody(local, values, runtime, controller, categories) }
        }
    }
}

@Composable
private fun SettingsPaneScaffold(runtime: SettingsPageRuntime, pane: SettingsPane, title: String,
    back: () -> Unit, search: (() -> Unit)?, content: @Composable () -> Unit) {
    if (pane == SettingsPane.Single) {
        TeakActivityScaffold(runtime.settings, back, options = ActivityPageOptions(title = title),
            slots = ActivityPageSlots(topBarActions = { if (search != null) SettingsCategorySearchButton(search) }), content = content)
    } else PageChromeScope {
        // 两栏共用主题，但外壳、滚动、弹层和玻璃采样各自持有，不能再套整页内容宽度限制。
        TeakPageScaffold(title = title, onBack = back, limitContentWidth = false,
            pageState = rememberTeakPageScrollState(), backgroundColor = MiuixTheme.colorScheme.surface,
            contentModifier = Modifier.background(MiuixTheme.colorScheme.surface).smartRefresh(runtime.settings.smartRefresh),
            topBarActions = { if (search != null) SettingsCategorySearchButton(search) },
            overlay = { ScrollTopButton(runtime.settings) }, content = content)
    }
}

@Composable
private fun SettingsPaneBody(session: SettingsBrowserSession, values: Map<String, SettingValue>, runtime: SettingsPageRuntime,
    controller: IndexedSettingsStateHolder, categories: Boolean) {
    val background = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop { drawRect(background); drawContent() }
    CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
        Scaffold(containerColor = Color.Transparent, contentWindowInsets = WindowInsets(0, 0, 0, 0)) {
            Box(Modifier.fillMaxSize().background(background)) {
                Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                    Box(Modifier.fillMaxSize().settingsUnderlay(session.navigation.searching, runtime.settings.animationsEnabled)) {
                        if (categories) SettingsCategoryList(session, Modifier.fillMaxSize(), runtime)
                        else SettingsCategoryContent(session, values, runtime, controller, Modifier.fillMaxSize())
                    }
                    if (session.navigation.searching) {
                        val hits = searchSettingsCategories(session.categories + session.footers, session.navigation.query, session.navigation.searchRoute)
                        SettingsSearchResults(session, hits, Modifier.fillMaxSize(), runtime)
                    }
                }
                if (categories || session.navigation.searching) SettingsSearchDock(session)
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
    return blockTouches(hidden).then(if (hidden) Modifier.clearAndSetSemantics { } else Modifier).teakSearchUnderlay(progress, alpha)
}
