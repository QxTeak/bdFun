package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.SettingsIndex
import com.aritxonly.bdfun.core.model.settings.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class IndexedSettingsStateHolderTest {
    @Test fun preservesOldValuesAndWritesThroughOriginalController() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(mutableMapOf("background_play" to false))
            val original = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            val setting = Setting("background_play", "boolean", "后台播放", null, SettingValue.Flag(true))
            val index = SettingsIndex(listOf(SettingsCategory("播放", "settings/playback", null,
                listOf(SettingsGroup("播放行为", listOf(setting))))))
            val controller = IndexedSettingsStateHolder(index, original, original.state, scope)
            assertEquals(SettingValue.Flag(false), controller.state.value.values["background_play"])
            assertEquals(SettingUpdate.Updated, controller.update("background_play", SettingValue.Flag(true)))
            assertTrue(original.backgroundPlay)
            assertEquals(true, prefs.values["background_play"])
            original.backgroundPlay = false
            assertEquals(SettingValue.Flag(false), controller.state.value.values["background_play"])
            controller.search("后台")
            assertEquals(1, controller.state.value.hits.size)
            assertEquals(SettingUpdate.Updated, controller.reset("background_play"))
            assertTrue(original.backgroundPlay)
        } finally { scope.cancel() }
    }

    @Test fun invalidValueOrUnknownKeyNeverWritesPreferences() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences()
            val original = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            val setting = Setting("quality", "select", "清晰度", null, SettingValue.Number(64),
                listOf(SettingOption("720P", SettingValue.Number(64)), SettingOption("1080P", SettingValue.Number(80))))
            val index = SettingsIndex(listOf(SettingsCategory("播放", "settings/playback", null,
                listOf(SettingsGroup("画质", listOf(setting))))))
            val controller = IndexedSettingsStateHolder(index, original, original.state, scope)
            val writes = prefs.applies
            assertTrue(controller.update("quality", SettingValue.Number(1)) is SettingUpdate.Rejected)
            assertTrue(controller.update("quality", SettingValue.Text("80")) is SettingUpdate.Rejected)
            assertTrue(controller.update("unknown", SettingValue.Flag(true)) is SettingUpdate.Rejected)
            assertEquals(writes, prefs.applies)
            controller.update("quality", SettingValue.Number(80))
            assertEquals(80, original.qualityPreference)
            assertEquals(80, prefs.values["quality"])
        } finally { scope.cancel() }
    }
}
