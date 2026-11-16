package com.aritxonly.bdfun.feature.settings.application

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SettingsMigrationTest {
    @Test fun legacyMigrationsRunOnceAndRetainSubsequentUserChoices() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(linkedMapOf("quality" to 80, "decoder_mode" to 6, "dynamic_auto_play" to false))
            val first = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(0, first.qualityPreference)
            assertEquals(3, first.decoderMode)
            assertFalse(first.dynamicAutoPlay)
            first.qualityPreference = 120
            first.decoderMode = 1
            first.recommendWeb = true
            first.cdnService = "ali"
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(120, restored.qualityPreference)
            assertEquals(1, restored.decoderMode)
            assertTrue(restored.recommendWeb)
            assertEquals("ali", restored.cdnService)
        } finally { scope.cancel() }
    }
    @Test fun legacyLowQualityAndFloatClampsArePreserved() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(linkedMapOf("quality" to 32))
            val state = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(64, state.qualityPreference)
            state.danmakuScaleInline = 10f
            state.danmakuArea = -1f
            state.defaultPlaybackSpeed = 9f
            assertEquals(2f, state.danmakuScaleInline)
            assertEquals(0.15f, state.danmakuArea)
            assertEquals(3f, state.defaultPlaybackSpeed)
            state.flush()
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(2f, restored.danmakuScaleInline)
        } finally { scope.cancel() }
    }
    @Test fun sliderChangesMergeIntoOnePersistenceOperation() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences()
            val state = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            val before = prefs.applies
            state.danmakuScaleInline = 1.1f
            state.danmakuScaleFullscreen = 1.5f
            state.danmakuScaleInline = 1.7f
            assertEquals(before, prefs.applies)
            state.flush()
            assertEquals(before + 1, prefs.applies)
            assertEquals(1.7f, prefs.values["dm_scale_inline"])
            assertEquals(1.5f, prefs.values["dm_scale_fullscreen"])
        } finally { scope.cancel() }
    }
    @Test fun historyRetainsOrderDeduplicatesLimitsAndReadsLegacySet() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(linkedMapOf("search_history" to linkedSetOf("old", "second")))
            val state = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(listOf("old", "second"), state.searchHistory)
            state.addSearchHistory("new")
            state.addSearchHistory("old")
            assertEquals(listOf("old", "new", "second"), state.searchHistory)
            repeat(20) { state.addSearchHistory("item$it") }
            assertEquals(12, state.searchHistory.size)
            assertEquals("item19", state.searchHistory.first())
            state.clearSearchHistory()
            assertTrue(state.searchHistory.isEmpty())
            assertFalse(prefs.contains("search_history"))
            assertFalse(prefs.contains("search_history_json"))
        } finally { scope.cancel() }
    }
    @Test fun hyperOs4StylePersistsAndLegacySearchToggleIsRemoved() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(linkedMapOf("bottom_search" to true))
            val state = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertFalse(prefs.contains("bottom_search"))
            assertEquals(2, state.interfaceStyle)
            state.interfaceStyle = 0
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(0, restored.interfaceStyle)
            restored.interfaceStyle = 8
            assertEquals(2, restored.interfaceStyle)
            restored.colorStyle = 8
            assertEquals(1, restored.colorStyle)
        } finally { scope.cancel() }
    }
}
