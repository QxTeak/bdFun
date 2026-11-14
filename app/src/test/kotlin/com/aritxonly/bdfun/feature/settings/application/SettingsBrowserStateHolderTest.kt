package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.SettingsIndex
import com.aritxonly.bdfun.core.model.settings.*
import org.junit.Assert.*
import org.junit.Test

class SettingsBrowserStateHolderTest {
    @Test fun backClosesSearchThenReturnsToCategories() {
        val page = SettingsBrowserStateHolder()
        page.open("settings/player")
        page.search("settings/player")
        page.query("解码")
        assertTrue(page.back())
        assertEquals("settings/player", page.state.value.route)
        assertFalse(page.state.value.searching)
        assertEquals("", page.state.value.query)
        assertTrue(page.back())
        assertNull(page.state.value.route)
        assertFalse(page.back())
    }

    @Test fun resultNavigationClosesSearchAndTargetsSetting() {
        val page = SettingsBrowserStateHolder()
        page.search()
        page.query("主题")
        page.open("settings/appearance", "dark_mode")
        assertFalse(page.state.value.searching)
        assertEquals("dark_mode", page.state.value.highlight)
        page.clearHighlight("unrelated")
        assertEquals("dark_mode", page.state.value.highlight)
        page.clearHighlight("dark_mode")
        assertNull(page.state.value.highlight)
    }

    @Test fun hiddenRowsAndEmptyCategoriesAreNotExposed() {
        val toggle = Setting("enabled", "switch", "开关", null, SettingValue.Flag(false))
        val dependent = Setting("side", "integer", "位置", null, SettingValue.Number(0), visibleWhen = "enabled")
        val action = Setting("logout", "action", "退出登录", null, SettingValue.Text(""), storage = "none", visibleWhen = "logged_in")
        val debug = action.copy(prefName = "logs", title = "日志", visibleWhen = "debug")
        val index = SettingsIndex(listOf(
            SettingsCategory("外观", "settings/appearance", null, listOf(SettingsGroup("显示", listOf(toggle, dependent)))),
            SettingsCategory("账号", "settings/account", null, listOf(SettingsGroup("账号", listOf(action, debug)))),
        ))
        val guest = visibleSettingsIndex(index, mapOf("enabled" to SettingValue.Flag(false)), false, false)
        assertEquals(1, guest.size)
        assertEquals(listOf("enabled"), guest.single().groups.single().settings.map { it.prefName })
        val full = visibleSettingsIndex(index, mapOf("enabled" to SettingValue.Flag(true)), true, true)
        assertEquals(4, full.flatMap { it.groups }.flatMap { it.settings }.size)
    }

    @Test fun optionalComponentsRequireRegisteredHosts() {
        val entry = Setting("recommendation_toggle", "extension", "推荐算法优化", null,
            SettingValue.Text(""), storage = "none", visibleWhen = "extension:recommendation_toggle")
        assertFalse(settingsVisible(entry, emptyMap(), true, true))
        assertTrue(settingsVisible(entry, emptyMap(), true, true, setOf("recommendation_toggle")))
    }

    @Test fun restoredNavigationDoesNotNeedASecondLayoutState() {
        val saved = SettingsBrowserState("settings/player", true, "settings/player", "蓝牙")
        assertEquals(saved, SettingsBrowserStateHolder(saved).state.value)
    }
}
