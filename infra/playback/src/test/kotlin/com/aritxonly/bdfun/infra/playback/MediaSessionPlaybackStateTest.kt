package com.aritxonly.bdfun.infra.playback

import android.support.v4.media.session.PlaybackStateCompat
import com.aritxonly.bdfun.core.application.*
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSessionPlaybackStateTest {
    @Test fun bufferingKeepsIntentButUserPauseWins() {
        val buffering = PlaybackSnapshot(phase = PlaybackPhase.Buffering, playWhenReady = true)
        assertEquals(PlaybackStateCompat.STATE_BUFFERING, mediaSessionPlaybackState(buffering))
        assertEquals(PlaybackStateCompat.STATE_PAUSED,
            mediaSessionPlaybackState(buffering.copy(playWhenReady = false)))
    }
    @Test fun endedErrorAndPlayingRemainDistinct() {
        assertEquals(PlaybackStateCompat.STATE_STOPPED,
            mediaSessionPlaybackState(PlaybackSnapshot(phase = PlaybackPhase.Ended, playWhenReady = true)))
        assertEquals(PlaybackStateCompat.STATE_ERROR,
            mediaSessionPlaybackState(PlaybackSnapshot(error = PlaybackFailure("network", null))))
        assertEquals(PlaybackStateCompat.STATE_PLAYING,
            mediaSessionPlaybackState(PlaybackSnapshot(playing = true, playWhenReady = true)))
    }
}
