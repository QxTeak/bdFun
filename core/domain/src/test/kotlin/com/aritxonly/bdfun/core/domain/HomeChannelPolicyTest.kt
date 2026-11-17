package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class HomeChannelPolicyTest {
    @Test fun recommendedCannotBeDisabledAndUnknownKeysAreIgnored() {
        assertEquals(setOf("recommend"), HomeChannelPolicy.normalize(emptySet()))
        assertEquals(setOf("recommend"), HomeChannelPolicy.normalize(setOf("unknown")))
    }
    @Test fun filteringKeepsStableIdentityAndOrder() {
        val visible = HomeChannelPolicy.visible(setOf("cinema", "live"))
        assertEquals(listOf("recommend", "live", "cinema"), visible.map { it.key })
        assertEquals(listOf(0, 1, 4), visible.map { it.ordinal })
    }
    @Test fun removingCurrentChannelFallsBackToRecommend() {
        assertEquals(0, HomeChannelPolicy.selected(4, setOf("live")))
        assertEquals(4, HomeChannelPolicy.selected(4, setOf("cinema")))
        assertEquals(0, HomeChannelPolicy.selected(99, HomeChannelPolicy.defaults))
    }
}
