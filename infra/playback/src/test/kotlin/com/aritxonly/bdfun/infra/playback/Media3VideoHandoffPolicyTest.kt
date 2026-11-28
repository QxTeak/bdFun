package com.aritxonly.bdfun.infra.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class Media3VideoHandoffPolicyTest {
    @Test fun stageAheadOfTheAudioClockAtTheCurrentSpeed() {
        assertEquals(12_000L, stagingPosition(10_000, 40_000, advancing = true, speed = 1f))
        assertEquals(14_000L, stagingPosition(10_000, 40_000, advancing = true, speed = 2f))
        assertEquals(10_000L, stagingPosition(10_000, 40_000, advancing = false, speed = 2f))
    }

    @Test fun keepTheStagingPointInsideTheMediaEvenNearTheEnd() {
        assertEquals(19_750L, stagingPosition(19_000, 20_000, advancing = true, speed = 1f))
        assertEquals(19_990L, stagingPosition(19_990, 20_000, advancing = true, speed = 1f))
    }

    @Test fun correctOnlyTheVideoClockAndKeepItsNominalSpeedWhenAligned() {
        assertEquals(1.5f, followingVideoSpeed(1.5f, 12), 0.001f)
        assertEquals(1.44f, followingVideoSpeed(1.5f, 60), 0.001f)
        assertEquals(1.56f, followingVideoSpeed(1.5f, -60), 0.001f)
        assertEquals(1.5f, followingVideoSpeed(1.5f, -500), 0.001f)
    }

    @Test fun restrictThePresentationGapUsingThePlaybackSpeed() {
        assertEquals(22L, handoffTolerance(playing = false, speed = 3f))
        assertEquals(32L, handoffTolerance(playing = true, speed = 1f))
        assertEquals(64L, handoffTolerance(playing = true, speed = 2f))
    }
}
