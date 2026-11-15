package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.model.settings.SettingValue
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RestoredSettingsBindingsTest {
    @Test fun restoredRowsReadExistingPreferencesAndKeepExitFlagsConsistent() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(mutableMapOf("recommend_web" to true, "home_card_style" to 1, "exit_video_control" to 0,
                "disc_breath_enabled" to true, "disc_breath_cover_color" to true, "stop_on_video_page_exit" to true))
            val original = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            val bindings = indexedSettingBindings(original)
            assertEquals(SettingValue.Number(1), bindings.getValue("home_card_style").read())
            assertEquals(SettingValue.Number(0), bindings.getValue("exit_video_control").read())
            assertEquals(SettingValue.Flag(true), bindings.getValue("disc_breath_enabled").read())
            assertEquals(SettingValue.Flag(true), bindings.getValue("disc_breath_cover_color").read())
            assertEquals(SettingValue.Flag(true), bindings.getValue("recommend_web").read())
            bindings.getValue("recommend_web").write(SettingValue.Flag(false))
            assertEquals(false, prefs.values["recommend_web"])
            val continued = bindings.getValue("show_disc_after_video")
            assertEquals(SettingValue.Flag(false), continued.read())
            continued.write(SettingValue.Flag(true))
            assertTrue(original.showDiscAfterVideo)
            assertFalse(original.stopOnVideoPageExit)
            continued.write(SettingValue.Flag(false))
            assertFalse(original.showDiscAfterVideo)
            assertTrue(original.stopOnVideoPageExit)
            bindings.getValue("exit_video_control").write(SettingValue.Number(1))
            assertEquals(1, prefs.values["exit_video_control"])
            bindings.getValue("home_card_style").write(SettingValue.Number(0))
            assertEquals(0, prefs.values["home_card_style"])
        } finally { scope.cancel() }
    }
}
