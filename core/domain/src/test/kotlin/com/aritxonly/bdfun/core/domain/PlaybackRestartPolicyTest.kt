package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackRestartPolicyTest {
    @Test fun endedPlaybackRestartsFromStart() {
        assertEquals(0L, playbackRestartPositionMs(ended = true))
    }

    @Test fun playingOrPausedKeepCurrentPosition() {
        assertNull(playbackRestartPositionMs(ended = false))
    }

    @Test fun endedPlaybackIsNotTreatedAsPlaying() {
        // 播完后 playWhenReady 仍是 true，但不能据此判成“正在播”，否则按播放会变成暂停。
        assertFalse(playbackIntentIsPlaying(ended = true, playWhenReady = true))
    }

    @Test fun playingAndPausedFollowPlayWhenReady() {
        assertTrue(playbackIntentIsPlaying(ended = false, playWhenReady = true))
        assertFalse(playbackIntentIsPlaying(ended = false, playWhenReady = false))
    }
}
