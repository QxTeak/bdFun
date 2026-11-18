package com.aritxonly.bdfun.core.domain

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class MultipartProgressTest {
    @Test fun partsHaveIndependentKeys() {
        assertNotEquals(playbackProgressKey("BV1", 11), playbackProgressKey("BV1", 12))
        assertEquals("BV1", playbackProgressKey("BV1", 0))
        assertEquals("", playbackProgressKey("", 11))
    }
    @Test fun resumeStillHonorsEndGuard() {
        assertEquals(12000L, resumePlaybackProgress("12000,60000,1"))
        assertEquals(0L, resumePlaybackProgress("59000,60000,1"))
    }
}
