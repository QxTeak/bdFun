package com.aritxonly.bdfun.core.domain

import org.junit.Test
import org.junit.Assert.assertEquals

class FeedRefreshRatePolicyTest {
    @Test fun supportedModesAndInvalidFallback() {
        for (value in listOf(0, 60, 120)) assertEquals(value, normalizeFeedRefreshRate(value))
        for (value in listOf(-1, 90, 144)) assertEquals(60, normalizeFeedRefreshRate(value))
        assertEquals(60, DEFAULT_FEED_REFRESH_RATE)
    }
}
