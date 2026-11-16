package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.application.reducePlaybackSnapshot
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlaybackSessionStateHolderTest {
    private fun test(block: (PlaybackSessionStateHolder) -> Unit) {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try { block(PlaybackSessionStateHolder(scope)) } finally { scope.cancel() }
    }
    @Test fun oldPageDetachCannotClearNewController() = test { session ->
        val old = Any(); val next = Any()
        val first = FakePlaybackController(); val second = FakePlaybackController()
        session.attach(old, first); session.attach(next, second)
        assertFalse(session.detach(old))
        assertSame(second, session.controller)
        assertTrue(session.state.value.active)
    }
    @Test fun handOffKeepsSameTransportAndProgressAfterPageDetach() = test { session ->
        val owner = Any(); val player = FakePlaybackController()
        session.attach(owner, player); player.seekTo(1234)
        session.requestHandOff()
        assertFalse(session.detach(owner))
        assertEquals(1234L, session.state.value.playback.positionMs)
        session.adopt(player)
        player.play(); player.seekTo(2345)
        assertTrue(session.state.value.detached)
        assertTrue(session.state.value.playback.playing)
        assertEquals(2345L, session.state.value.playback.positionMs)
        assertFalse(player.released)
    }
    @Test fun detachedControlsShareOriginalPlayer() = test { session ->
        val player = FakePlaybackController()
        session.attach(Any(), player); session.adopt(player)
        session.controller!!.play(); session.controller!!.setSpeed(2f); session.controller!!.seekTo(999)
        assertEquals(listOf("play", "speed", "seek"), player.commands)
        assertEquals(2f, session.state.value.playback.speed)
        assertEquals(999L, session.state.value.playback.positionMs)
    }
    @Test fun clearedSessionRejectsFurtherOldPlayerUpdates() = test { session ->
        val player = FakePlaybackController()
        session.attach(Any(), player); session.clear(); player.play()
        assertFalse(session.state.value.active)
        assertFalse(session.state.value.playback.playing)
        assertNull(session.controller)
    }
    @Test fun replacedPlayersUpdatesDoNotLeakToCurrentSession() = test { session ->
        val old = FakePlaybackController(); val current = FakePlaybackController()
        session.attach(Any(), old); session.attach(Any(), current)
        old.seekTo(999); current.seekTo(100)
        assertEquals(100L, session.state.value.playback.positionMs)
    }
    @Test fun oldPageCannotClearCurrentFullscreenRateHint() = test { session ->
        val old = Any(); val current = Any()
        session.attach(old, FakePlaybackController())
        session.setFullscreen(old, true)
        session.attach(current, FakePlaybackController())
        assertFalse(session.state.value.fullscreen)
        session.setFullscreen(current, true)
        session.setFullscreen(old, false)
        assertTrue(session.state.value.fullscreen)
    }
    @Test fun acknowledgedEndIsNotReplayedByPolling() {
        val ended = reducePlaybackSnapshot(PlaybackSnapshot(phase = PlaybackPhase.Ready),
            PlaybackPhase.Ended, false, true, 1000, 1000, 1000, 1f)
        assertTrue(ended.ended)
        val acknowledged = ended.copy(ended = false)
        val tick = reducePlaybackSnapshot(acknowledged, PlaybackPhase.Ended, false, true, 1000, 1000, 1000, 1f)
        assertFalse(tick.ended)
    }

    @Test fun newVideoPausesPreviousTransportButSamePlayerHandOffDoesNot() = test { session ->
        val old = FakePlaybackController()
        session.attach(Any(), old)
        old.play()
        val next = FakePlaybackController()
        session.attach(Any(), next)
        assertFalse(old.state.value.playWhenReady)
        assertTrue(old.commands.contains("pause"))
        next.play()
        session.adopt(next)
        assertTrue(next.state.value.playWhenReady)
        assertFalse(next.commands.contains("pause"))
    }
}
