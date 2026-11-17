package com.aritxonly.bdfun.core.domain.buffering

import org.junit.Assert.*
import org.junit.Test

class BufferPolicyTest {
    @Test fun fixedStagesUseActualWatchTime() {
        val strategy = FixedCacheStrategy()
        assertEquals(8_000L, strategy.budget(BufferContext(7_999)).targetMs)
        assertEquals(20_000L, strategy.budget(BufferContext(8_000)).targetMs)
        assertEquals(35_000L, strategy.budget(BufferContext(30_000)).targetMs)
    }

    @Test fun hysteresisAndSpeedUseWallClockReserve() {
        val provider = VideoBufferProvider(FixedCacheStrategy())
        val context = BufferContext(0)
        fun decide(buffer: Long, speed: Float = 1f) = provider.decide(context, BufferInput(buffer, speed, 0))
        assertTrue(decide(0).shouldLoad)
        assertTrue(decide(7_000).shouldLoad)
        assertFalse(decide(8_000).shouldLoad)
        assertFalse(decide(5_000).shouldLoad)
        assertTrue(decide(5_000, 2f).shouldLoad)
        assertEquals(16_000L, decide(5_000, 2f).targetMediaMs)
        assertEquals(11_000L, decide(5_000, 2f).additionalMediaMs)
        assertEquals(24_000L, decide(0, 3f).targetMediaMs)
    }

    @Test fun pauseMemoryAndRebufferSafetyApplyToBothStrategies() {
        for (strategy in listOf(FixedCacheStrategy(), AdaptiveCacheStrategy { WatchHistory() })) {
            val provider = VideoBufferProvider(strategy)
            assertFalse(provider.decide(BufferContext(0), BufferInput(0, 1f,
                VideoBufferProvider.MAX_BUFFER_BYTES)).shouldLoad)
            assertFalse(provider.decide(BufferContext(0), BufferInput(2_000, 1f, 0, false)).shouldLoad)
            assertTrue(provider.decide(BufferContext(0), BufferInput(0, 1f, 0, false)).shouldLoad)
            val recovery = provider.decide(BufferContext(0, true), BufferInput(0, 2f, 0))
            assertEquals(50_000L, recovery.targetMediaMs)
            assertEquals(20_000L, recovery.lowWaterMediaMs)
            assertTrue(recovery.shouldLoad)
            assertTrue(provider.decide(BufferContext(100_000), BufferInput(0, 10f, 0)).targetMediaMs <= 120_000)
        }
    }

    @Test fun coldStartEqualsFixedAndHistoryChangesBudgetInExpectedDirection() {
        val context = BufferContext(0)
        assertEquals(FixedCacheStrategy().budget(context), AdaptiveCacheStrategy { WatchHistory() }.budget(context))
        var quick = WatchHistory()
        var sustained = WatchHistory()
        repeat(150) {
            quick = quick.observe(2_000, true)
            sustained = sustained.observe(120_000, false)
        }
        val shortBudget = AdaptiveCacheStrategy { quick }.budget(context)
        val longBudget = AdaptiveCacheStrategy { sustained }.budget(context)
        assertTrue(shortBudget.targetMs < 8_000)
        assertTrue(longBudget.targetMs > 8_000)
        assertTrue(shortBudget.targetMs >= 6_000)
        assertTrue(sustained.survival(15_000, 30_000) < sustained.survival(15_000, 5_000))
    }

    @Test fun censoredObservationAddsExposureWithoutExitAndHistoryDecays() {
        val censored = WatchHistory().observe(10_000, false)
        assertEquals(0.0, censored.exits.sum(), 0.0)
        assertEquals(10.0, censored.exposureSeconds.sum(), 0.0)
        val exit = censored.observe(2_000, true)
        assertEquals(1.0, exit.exits[0], 0.0)
        assertEquals(11.8, exit.exposureSeconds.sum(), 0.00001)
        assertEquals(1.98, exit.samples, 0.00001)
    }
}
