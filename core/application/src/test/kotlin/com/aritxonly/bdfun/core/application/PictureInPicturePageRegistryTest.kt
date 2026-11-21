package com.aritxonly.bdfun.core.application

import org.junit.Assert.assertEquals
import org.junit.Test

class PictureInPicturePageRegistryTest {
    private val old = Any()
    private val new = Any()

    @Test fun newPageClosesExistingPictureInPicturePage() {
        val registry = PictureInPicturePageRegistry(); var closed = 0
        registry.enter(old) { closed++ }
        registry.closeOthers(new)
        assertEquals(1, closed)
    }

    @Test fun pageDoesNotCloseItself() {
        val registry = PictureInPicturePageRegistry(); var closed = 0
        registry.enter(old) { closed++ }
        registry.closeOthers(old)
        assertEquals(0, closed)
    }

    @Test fun exitedPageIsNotClosedLater() {
        val registry = PictureInPicturePageRegistry(); var closed = 0
        registry.enter(old) { closed++ }
        registry.exit(old)
        registry.closeOthers(new)
        assertEquals(0, closed)
    }

    @Test fun closedPageIsClosedOnlyOnce() {
        val registry = PictureInPicturePageRegistry(); var closed = 0
        registry.enter(old) { closed++ }
        registry.closeOthers(new); registry.closeOthers(Any())
        assertEquals(1, closed)
    }

    @Test fun reenteringReplacesCallbackInsteadOfDuplicating() {
        val registry = PictureInPicturePageRegistry(); var first = 0; var second = 0
        registry.enter(old) { first++ }; registry.enter(old) { second++ }
        registry.closeOthers(new)
        assertEquals(0, first); assertEquals(1, second)
    }

    @Test fun failingCloseDoesNotBlockOtherPages() {
        val registry = PictureInPicturePageRegistry(); var closed = 0
        registry.enter(old) { error("boom") }; registry.enter(Any()) { closed++ }
        registry.closeOthers(new)
        assertEquals(1, closed)
    }
}
