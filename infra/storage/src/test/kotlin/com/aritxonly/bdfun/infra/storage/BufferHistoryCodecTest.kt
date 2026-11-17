package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.buffering.WatchHistory
import org.junit.Assert.*
import org.junit.Test

class BufferHistoryCodecTest {
    @Test fun aggregateSurvivesRoundTrip() {
        val history = WatchHistory().observe(13_000, true).observe(80_000, false)
        assertEquals(history, BufferHistoryCodec.decode(BufferHistoryCodec.encode(history)))
    }

    @Test fun corruptOrNonFiniteDataFallsBackToColdStart() {
        val valid = BufferHistoryCodec.encode(WatchHistory())
        for (raw in listOf(null, "", "1,2", "$valid,bad", valid.replaceFirst("0.0", "NaN"),
            valid.replaceFirst("0.0", "Infinity"), valid.replaceFirst("0.0", "-1"))) {
            assertEquals(WatchHistory(), BufferHistoryCodec.decode(raw))
        }
    }
}
