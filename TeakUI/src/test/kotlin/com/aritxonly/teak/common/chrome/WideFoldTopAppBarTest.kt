package com.aritxonly.teak.common.chrome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WideFoldTopAppBarTest {
    @Test fun `short wide cover and unfolded window use fixed title`() {
        assertTrue(usesFixedWideFoldTopBar(432f, 634f))
        assertTrue(usesFixedWideFoldTopBar(600f, 720f))
        assertTrue(usesFixedWideFoldTopBar(720f, 600f))
        assertTrue(usesFixedWideFoldTopBar(1280f, 800f))
        assertTrue(usesFixedWideFoldTopBar(800f, 1280f))
    }

    @Test fun `tall and narrow phone preserve upstream title behavior`() {
        assertFalse(usesFixedWideFoldTopBar(412f, 892f))
        assertFalse(usesFixedWideFoldTopBar(393f, 600f))
        assertTrue(usesFixedWideFoldTopBar(840f, 1000f))
        assertTrue(usesFixedWideFoldTopBar(720f, 360f))
    }

    @Test fun `window resize crosses the width and aspect boundaries`() {
        assertTrue(usesFixedWideFoldTopBar(400f, 660f))
        assertFalse(usesFixedWideFoldTopBar(400f, 661f))
        assertFalse(usesFixedWideFoldTopBar(399f, 600f))
        assertTrue(usesFixedWideFoldTopBar(839f, 700f))
        assertFalse(usesFixedWideFoldTopBar(0f, 0f))
    }
}
