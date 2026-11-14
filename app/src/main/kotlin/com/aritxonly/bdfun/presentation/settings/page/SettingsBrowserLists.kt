package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.*
import com.aritxonly.bdfun.feature.settings.application.*
import com.aritxonly.teak.common.settings.TeakSettingsCard
import com.aritxonly.teak.common.chrome.*
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal data class SettingsBrowserSession(
    val browser: SettingsBrowserStateHolder,
    val navigation: SettingsBrowserState,
    val selected: SettingsCategory?,
    val wide: Boolean,
    val categories: List<SettingsCategory>,
    val footers: List<SettingsCategory> = emptyList(),
)

@Composable
internal fun SettingsCategoryList(session: SettingsBrowserSession, modifier: Modifier, runtime: SettingsPageRuntime) {
    val list = rememberLazyListState()
    val chrome = LocalTeakPageScrollState.current
    if (!session.navigation.searching) TrackTeakPageScroll(list, chrome)
    LazyColumn(modifier, state = list, contentPadding = settingsListPadding()) {
        item { TeakLargeTitle("设置", pageState = chrome) }
        item {
            TeakSettingsCard {
                session.categories.forEach { category ->
                    val selected = session.wide && session.selected?.route == category.route
                    ArrowPreference(title = category.name,
                        modifier = if (selected) Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = .08f)) else Modifier,
                        onClick = { session.browser.open(category.route) })
                }
            }
        }
        session.footers.forEach { footer ->
            item(key = footer.route) {
                Spacer(Modifier.height(16.dp))
                TeakSettingsCard {
                    footer.groups.flatMap { it.settings }.forEach { IndexedActionComponent(it, runtime) }
                }
            }
        }
        if (session.categories.isEmpty()) item { Text("暂无设置", Modifier.padding(24.dp)) }
    }
}

@Composable
internal fun SettingsCategoryContent(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, modifier: Modifier) {
    val category = session.selected ?: return
    val list = key(category.route) { rememberLazyListState() }
    if (category.layout != "about" && !session.navigation.searching) TrackTeakPageScroll(list)
    LaunchedEffect(category.route, session.navigation.highlight) {
        val key = session.navigation.highlight ?: return@LaunchedEffect
        val group = category.groups.indexOfFirst { g -> g.settings.any { it.prefName == key } }
        if (group >= 0) list.animateScrollToItem(group + 1)
    }
    if (category.layout == "about") {
        SettingsAboutContent(session, values, runtime, controller, modifier)
        return
    }
    LazyColumn(modifier, state = list, contentPadding = settingsListPadding()) {
        item { TeakLargeTitle(category.name) }
        items(category.groups) { group -> SettingsIndexedGroup(group, session, values, runtime, controller) }
    }
}

@Composable
internal fun SettingsSearchResults(session: SettingsBrowserSession, hits: List<SettingsSearchHit>, modifier: Modifier, runtime: SettingsPageRuntime) {
    val list = rememberLazyListState()
    TrackTeakPageScroll(list)
    LazyColumn(modifier.imePadding().background(MiuixTheme.colorScheme.surface), state = list, contentPadding = settingsListPadding()) {
        item { TeakLargeTitle("搜索设置") }
        if (session.navigation.query.isBlank()) {
            item { Text("搜索名称、说明或关键词", Modifier.padding(20.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        } else if (hits.isEmpty()) {
            item { Text("没有找到相关设置", Modifier.padding(20.dp), color = MiuixTheme.colorScheme.onSurfaceVariantSummary) }
        } else {
            items(hits, key = { it.setting.prefName }) { hit ->
                TeakSettingsCard {
                    if (hit.category.layout == "root_footer") IndexedActionComponent(hit.setting, runtime)
                    else ArrowPreference(title = hit.setting.title, summary = "${hit.category.name} · ${hit.group.title}",
                        onClick = { session.browser.open(hit.category.route, hit.setting.prefName) })
                }
            }
        }
    }
}

@Composable
internal fun settingsListPadding(): PaddingValues {
    val horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 0.dp else TeakChromeDefaults.ContentHorizontalPadding
    return PaddingValues(start = horizontal, end = horizontal,
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TeakChromeDefaults.ContentTopPadding,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp)
}

@Composable
internal fun SettingsIndexedGroup(group: SettingsGroup, session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, aboutProgress: Float? = null) {
    if (group.settings.all { it.type == "app_identity" }) {
        group.settings.forEach { SettingsAboutAppHeader(it.title) }
        return
    }
    SettingsIndexGroupCard(group.title, aboutProgress) {
        group.settings.forEach { setting ->
            key(setting.prefName) {
                val highlight = session.navigation.highlight == setting.prefName
                val requester = remember { BringIntoViewRequester() }
                LaunchedEffect(highlight) { if (highlight) { withFrameNanos { }; requester.bringIntoView() } }
                Column(Modifier.bringIntoViewRequester(requester).then(
                    if (highlight) Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = .1f)) else Modifier)) {
                    IndexedSettingComponent(setting, values[setting.prefName], runtime, controller)
                }
            }
        }
    }
}
