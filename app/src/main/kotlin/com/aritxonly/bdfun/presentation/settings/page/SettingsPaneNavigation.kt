package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.settings.SettingValue
import com.aritxonly.bdfun.feature.settings.application.IndexedSettingsStateHolder
import top.yukonga.miuix.kmp.nav.core.*
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

private data class SettingsPaneRoute(val category: String?, val detail: String? = null) : NavKey

/** 保存由页面控制器负责；Miuix仅持有相同的瞬时栈，用原生转场渲染每层完整Scaffold。 */
@Composable
internal fun SettingsPaneNavigation(session: SettingsBrowserSession, values: Map<String, SettingValue>,
    runtime: SettingsPageRuntime, controller: IndexedSettingsStateHolder, pane: SettingsPane, modifier: Modifier) {
    val base = if (pane == SettingsPane.Single) null else session.selected?.route
    val desired = buildList {
        add(SettingsPaneRoute(base))
        if (pane == SettingsPane.Single && session.selected != null) add(SettingsPaneRoute(session.selected.route))
        session.navigation.detail?.let { add(SettingsPaneRoute(session.selected?.route, it)) }
    }
    val stack = remember { navBackStackOf(*desired.toTypedArray()) }
    SideEffect { if (stack.toList() != desired) { stack.clear(); stack.addAll(desired) } }
    // 单栏（手机）是整窗转场，圆角要贴住设备屏幕；双栏只在一个窗格里动，保持直角。
    // 半径复用 Miuix 的 rememberNavSystemCornerRadius，不另写一套设备圆角探测。
    val systemCornerRadius = rememberNavSystemCornerRadius()
    val effects = remember(systemCornerRadius, pane) {
        NavDisplayEffects(
            enableCornerClip = pane == SettingsPane.Single,
            cornerClipRadius = if (pane == SettingsPane.Single) systemCornerRadius else 0.dp,
        )
    }
    NavDisplay(stack, modifier, onBack = { if (!session.browser.back()) runtime.navigation.onBack() },
        transition = if (runtime.settings.animationsEnabled) NavTransitions.MiuixDefault else NavTransitions.None,
        effects = effects) {
        entry<SettingsPaneRoute> { key ->
            val current = key == desired.last()
            val page = session.copy(selected = session.categories.firstOrNull { it.route == key.category },
                navigation = session.navigation.copy(route = key.category, detail = key.detail,
                    searching = current && session.navigation.searching))
            SettingsBrowserPane(page, values, runtime, controller, pane, Modifier.fillMaxSize())
        }
    }
}
