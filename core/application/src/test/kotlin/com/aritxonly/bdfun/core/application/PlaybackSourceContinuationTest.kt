package com.aritxonly.bdfun.core.application

import org.junit.Assert.*
import org.junit.Test

class PlaybackSourceContinuationTest {
    private val source = PlaybackSource("cdn", userAgent = "test", mediaKey = "BV:1", resumePositionMs = 5000)
    private val stalled = PlaybackSnapshot(positionMs = 92000, playing = false, playWhenReady = true,
        phase = PlaybackPhase.Idle, speed = 1.5f, error = PlaybackFailure("IO", "timeout"))

    @Test fun exhaustedBufferRecoveryKeepsLivePositionAndIntent() {
        val next = continuePlaybackSource("BV:1", source, stalled)
        assertEquals(92000L, next.resumePositionMs)
        assertTrue(next.autoPlay)
        assertEquals(1.5f, next.speed)
    }
    @Test fun pausedQualityChangeRemainsPaused() {
        assertFalse(continuePlaybackSource("BV:1", source, stalled.copy(playWhenReady = false)).autoPlay)
    }
    @Test fun newPartAndNewVideoUseTheirOwnResumePosition() {
        assertEquals(source, continuePlaybackSource("BV:2", source, stalled))
        assertEquals(source, continuePlaybackSource("OTHER:1", source, stalled))
        assertEquals(source, continuePlaybackSource(null, source, stalled))
    }
    @Test fun unkeyedBackgroundQueueDoesNotInheritPreviousTrack() {
        val unkeyed = source.copy(mediaKey = null)
        assertEquals(unkeyed, continuePlaybackSource(null, unkeyed, stalled))
    }
    @Test fun repeatedRecoveryUsesLatestPositionIncludingExplicitRewind() {
        assertEquals(0L, continuePlaybackSource("BV:1", source, stalled.copy(positionMs = 0)).resumePositionMs)
        assertEquals(101000L, continuePlaybackSource("BV:1", source, stalled.copy(positionMs = 101000)).resumePositionMs)
    }
}
