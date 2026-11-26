package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class VideoOrientationPolicyTest {
    @Test fun compactPhoneLandscapeEntersFullscreen() {
        assertEquals(true, fullscreenAfterVideoRotation(true, 393, true))
        assertEquals(true, fullscreenAfterVideoRotation(true, 599, true))
    }
    @Test fun expandedFoldableKeepsLandscapePageAndManualFullscreen() {
        assertNull(fullscreenAfterVideoRotation(true, 600, true))
        assertNull(fullscreenAfterVideoRotation(true, 840, true))
    }
    @Test fun disabledRotationAndUnknownSizeDoNotForceFullscreen() {
        assertNull(fullscreenAfterVideoRotation(true, 393, false))
        assertNull(fullscreenAfterVideoRotation(true, 0, true))
    }
    @Test fun portraitLeavesFullscreenOnEitherDevice() {
        assertEquals(false, fullscreenAfterVideoRotation(false, 393, true))
        assertEquals(false, fullscreenAfterVideoRotation(false, 840, false))
    }

    @Test fun halfScreenCompactPhoneRotatingToLandscapeRequestsLandscape() {
        assertEquals(
            VideoRotationTarget.Landscape,
            rotationTargetForPose(landscape = true, fullscreen = false, smallestWidthDp = 393,
                autoRotate = true, orientationLocked = false),
        )
        assertEquals(
            VideoRotationTarget.Landscape,
            rotationTargetForPose(landscape = true, fullscreen = false, smallestWidthDp = 599,
                autoRotate = true, orientationLocked = false),
        )
    }

    @Test fun halfScreenPortraitPoseDoesNothing() {
        // 半屏本来就是锁竖屏，这里再请求竖屏只会和 idle 锁互相打架。
        assertNull(rotationTargetForPose(landscape = false, fullscreen = false, smallestWidthDp = 393,
            autoRotate = true, orientationLocked = false))
    }

    @Test fun tabletHalfScreenNeverStealsSystemRotation() {
        assertNull(rotationTargetForPose(landscape = true, fullscreen = false, smallestWidthDp = 840,
            autoRotate = true, orientationLocked = false))
        assertNull(rotationTargetForPose(landscape = true, fullscreen = false, smallestWidthDp = 0,
            autoRotate = true, orientationLocked = false))
    }

    @Test fun fullscreenFollowsPoseBothWays() {
        assertEquals(VideoRotationTarget.Landscape,
            rotationTargetForPose(true, true, 840, true, false))
        assertEquals(VideoRotationTarget.Portrait,
            rotationTargetForPose(false, true, 393, true, false))
    }

    @Test fun disabledOrLockedRotationDoesNothing() {
        assertNull(rotationTargetForPose(true, true, 393, false, false))
        assertNull(rotationTargetForPose(true, false, 393, true, true))
    }
}
