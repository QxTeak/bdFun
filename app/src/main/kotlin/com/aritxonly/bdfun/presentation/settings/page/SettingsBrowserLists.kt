package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

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
    val seperation: List<SettingsSeperation> = emptyList(),
)

@Composable
internal fun SettingsCategoryList(session: SettingsBrowserSession, modifier: Modifier, runtime: SettingsPageRuntime) {
    val list = rememberLazyListState()
    val chrome = LocalTeakPageScrollState.current
    // 卡片自身在非经典样式下带12dp顶距，补足到16dp，避免叠成28dp。
    val cardGap = if (TeakUiStyle.isClassic) 16.dp else 4.dp
    if (!session.navigation.searching) TrackTeakPageScroll(list, chrome)
    LazyColumn(modifier, state = list, contentPadding = settingsListPadding()) {
        item { TeakLargeTitle("设置", pageState = chrome) }
        session.seperation.forEach { group ->
            val entries = group.entries.mapNotNull { entry ->
                session.categories.firstOrNull { it.route == entry.route }?.let { entry to it }
            }
            if (entries.isNotEmpty()) item(key = group.id) {
                if (group != session.seperation.first()) Spacer(Modifier.height(cardGap))
                TeakSettingsCard {
                    entries.forEach { (entry, category) ->
                        val selected = session.wide && session.selected?.route == category.route
                        ArrowPreference(title = category.name, startAction = { SettingsCategoryIcon(entry) },
                            modifier = if (selected) Modifier.background(MiuixTheme.colorScheme.primary.copy(alpha = .08f)) else Modifier,
                            onClick = { session.browser.open(category.route) })
                        if (entry.route == "settings/about") {
                            // 捐赠从「关于」页挪到设置第一页，和关于同一张卡片、排在关于下面；
                            // 图标先直接复用关于的。
                            ArrowPreference(title = "捐赠", startAction = { SettingsCategoryIcon(entry) },
                                onClick = { runtime.navigation.onAbout(SettingsDestination.Donation) })
                        }
                    }
                }
            }
        }
        session.footers.forEach { footer ->
            item(key = footer.route) {
                Spacer(Modifier.height(cardGap))
                TeakSettingsCard {
                    footer.groups.flatMap { it.settings }.forEach { IndexedActionComponent(it, runtime) }
                }
            }
        }
        if (session.categories.isEmpty()) item { Text("暂无设置", Modifier.padding(24.dp)) }
        item(key = "settings-bounce-tail") { Spacer(Modifier.height(1.dp)) }
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
        // 首页频道不再单开一页：在本分类里用一个小标题直接展开开关和顺序。
        if (category.route == "settings/feeds") {
            item(key = "home-channels-inline") { HomeChannelsSettingsSection(runtime.settings) }
            item(key = "dynamic-tabs-inline") { DynamicTabsSettingsSection(runtime.settings) }
        }
    }
}

@Composable
internal fun SettingsSearchResults(session: SettingsBrowserSession, hits: List<SettingsSearchHit>, modifier: Modifier, runtime: SettingsPageRuntime) {
    val list = rememberLazyListState()
    if (session.navigation.searching) TrackTeakPageScroll(list)
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
    val horizontal = TeakUiStyle.design.contentInset(TeakChromeDefaults.ContentHorizontalPadding)
    return PaddingValues(start = horizontal, end = horizontal,
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + TeakChromeDefaults.ContentTopPadding,
        bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 100.dp)
}

@Composable
internal fun SettingsIndexedGroup(group: SettingsGroup, session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, aboutProgress: Float? = null) {
    if (group.settings.all { it.type == "app_identity" }) {
        group.settings.forEach { SettingsAboutAppHeader(it.title) { runtime.navigation.onAbout(SettingsDestination.Update) } }
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
