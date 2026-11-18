package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.HomeChannelPolicy
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class HomeChannelsSettingsTest {
    @Test fun channelsPersistAndMandatoryChannelSurvivesReload() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences(mutableMapOf())
            val settings = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(HomeChannelPolicy.defaults, settings.homeChannels)
            settings.homeChannels = setOf("cinema", "unknown")
            assertEquals(setOf("recommend", "cinema"), prefs.values["home_channels"])
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals(setOf("recommend", "cinema"), restored.homeChannels)
            restored.homeChannels = emptySet()
            assertEquals(setOf("recommend"), restored.homeChannels)
        } finally { scope.cancel() }
    }
}
