package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.*
import org.junit.Assert.*
import org.junit.Test

class SettingsSeperationIndexTest {
    private val index = SettingsIndex(listOf(
        SettingsCategory("外观", "settings/appearance", null, emptyList()),
        SettingsCategory("播放器", "settings/player", null, emptyList()),
        SettingsCategory("账号", "settings/account", null, emptyList(), "root_footer")))
    private val appearance = SettingsEntry("settings/appearance", "Background", "#FF9F0A")
    private val player = SettingsEntry("settings/player", "Play", "#3482FF")
    @Test fun groupsReferenceEachSecondaryPageExactlyOnceAndPreserveOrder() {
        val groups = listOf(SettingsSeperation("watch", "观看", listOf(player)), SettingsSeperation("look", "外观", listOf(appearance)))
        assertEquals(groups, SettingsSeperationIndex(groups, index).groups)
    }
    @Test fun invalidReferencesIconsColorsAndDuplicateGroupsAreRejected() {
        val valid = SettingsSeperation("all", "全部", listOf(appearance, player))
        val invalid = listOf(
            listOf(valid.copy(entries = listOf(appearance))),
            listOf(valid.copy(entries = listOf(appearance, player, player))),
            listOf(valid.copy(entries = listOf(appearance, player.copy(route = "settings/account")))),
            listOf(valid.copy(entries = listOf(appearance, player.copy(icon = "Unknown")))),
            listOf(valid.copy(entries = listOf(appearance, player.copy(color = "red")))),
            listOf(valid, valid), emptyList())
        invalid.forEach { assertThrows(IllegalArgumentException::class.java) { SettingsSeperationIndex(it, index) } }
    }
}
