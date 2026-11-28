package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FullscreenInteractionPolicyTest {
    @Test fun landscapeKeepsFullscreenAndOpensPanel() {
        assertEquals(
            FullscreenCommentPresentation.InlinePanel,
            fullscreenCommentPresentation(windowLandscape = true),
        )
    }

    @Test fun portraitExitsFullscreenToCommentSheet() {
        assertEquals(
            FullscreenCommentPresentation.ExitToDetail,
            fullscreenCommentPresentation(windowLandscape = false),
        )
    }
}
