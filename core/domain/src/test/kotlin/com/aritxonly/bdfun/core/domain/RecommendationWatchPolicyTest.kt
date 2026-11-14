package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class RecommendationWatchPolicyTest {
    @Test fun pauseBufferAndSeekDoNotCountAsViewing() {
        val watch = RecommendationWatchPolicy()
        watch.sample(0, 0, true, 1f)
        watch.sample(1000, 1000, true, 1f)
        watch.sample(50_000, 2000, true, 1f)
        watch.sample(50_000, 3000, false, 1f)
        watch.sample(51_000, 4000, true, 1f)
        assertEquals(1000L, watch.contentTimeMs)
        assertEquals(1000L, watch.wallTimeMs)
    }
    @Test fun repeatDoesNotCountTheSameContentTwice() {
        val watch = RecommendationWatchPolicy()
        watch.sample(0, 0, true, 1f)
        watch.sample(1000, 1000, true, 1f)
        watch.sample(0, 2000, true, 1f)
        watch.sample(1000, 3000, true, 1f)
        assertEquals(1000L, watch.contentTimeMs)
    }
    @Test fun speedKeepsContentAndWallClockSeparate() {
        val watch = RecommendationWatchPolicy()
        for (i in 0..10) watch.sample(i * 2000L, i * 1000L, true, 2f)
        assertEquals(20_000L, watch.contentTimeMs)
        assertEquals(10_000L, watch.wallTimeMs)
        assertEquals(1.0, watch.satisfaction(20_000), 0.0)
    }
    @Test fun shortAccidentalClickIsNotPositiveEvidence() {
        val watch = RecommendationWatchPolicy()
        for (i in 0..3) watch.sample(i * 1000L, i * 1000L, true, 1f)
        assertEquals(0.0, watch.satisfaction(3000), 0.0)
    }
    @Test fun longViewingCanBeSatisfiedWithoutCompletingLongVideo() {
        val watch = RecommendationWatchPolicy()
        for (i in 0..300) watch.sample(i * 1000L, i * 1000L, true, 1f)
        assertEquals(1.0, watch.satisfaction(3_600_000), 0.0)
    }
    @Test fun invalidTimeAndSpeedCannotCreateCredit() {
        val watch = RecommendationWatchPolicy()
        watch.sample(0, 1000, true, 1f)
        watch.sample(1000, 0, true, 1f)
        watch.sample(2000, 1000, true, Float.NaN)
        assertEquals(0L, watch.contentTimeMs)
    }
}
