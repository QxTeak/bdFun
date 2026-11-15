package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.settings.*
import org.junit.Assert.*
import org.junit.Test

class SettingsVisibilityTest {
    private val toggle = Setting("continue_play", "switch", "继续播放", null, SettingValue.Flag(true))
    private val control = Setting("control", "dropdown", "控件", null, SettingValue.Number(1),
        listOf(SettingOption("画中画", SettingValue.Number(0)), SettingOption("唱片", SettingValue.Number(1))))
    private val entries = listOf(toggle, control).associateBy { it.prefName }
    @Test fun acceptsOnlyDeclaredBooleanKeysAndIntegerOptions() {
        assertTrue(SettingsVisibility.valid("continue_play&&control=1", entries))
        listOf("missing", "control", "control=2", "continue_play=1", "control=1=1", "").forEach {
            assertFalse(it, SettingsVisibility.valid(it, entries))
        }
    }
    @Test fun discOnlyVisibleWhenPlaybackIsEnabledAndDiscSelected() {
        fun matches(enabled: Boolean, value: Int) = SettingsVisibility.matches("continue_play&&control=1",
            mapOf("continue_play" to SettingValue.Flag(enabled), "control" to SettingValue.Number(value)))
        assertTrue(matches(true, 1))
        assertFalse(matches(true, 0))
        assertFalse(matches(false, 1))
        assertFalse(SettingsVisibility.matches("continue_play&&control=1", emptyMap()))
    }
}
