package com.aritxonly.bdfun.feature.settings.application

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class TabClickSettingsTest {
    @Test fun choicePersistsAcrossRecreation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences()
            val settings = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertFalse(settings.tabClickRefresh)
            settings.tabClickRefresh = true
            assertEquals(true, prefs.values["tab_click_refresh"])
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertTrue(restored.tabClickRefresh)
            restored.tabClickRefresh = false
            assertFalse(SettingsStateHolder.create(prefs, TestHistoryCodec, scope).tabClickRefresh)
        } finally { scope.cancel() }
    }
}
