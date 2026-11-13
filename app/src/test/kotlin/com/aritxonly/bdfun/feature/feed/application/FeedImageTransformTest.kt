package com.aritxonly.bdfun.feature.feed.application

import org.junit.Assert.*
import org.junit.Test

class FeedImageTransformTest {
    @Test fun unzoomedSinglePointerRemainsAvailableToPager() {
        assertEquals(FeedImageTransform(1f, 0f, 0f, false), dynamicImageTransform(1f, 8f, -4f, 1, 2f, 20f, 30f))
    }

    @Test fun pinchClampsAtFourAndAccumulatesPan() {
        assertEquals(FeedImageTransform(4f, 13f, -2f, true), dynamicImageTransform(3f, 8f, -4f, 2, 2f, 5f, 2f))
    }

    @Test fun pinchingBackToOriginalSizeClearsPanButConsumesGesture() {
        assertEquals(FeedImageTransform(1f, 0f, 0f, true), dynamicImageTransform(2f, 80f, 40f, 2, 0.25f, 10f, 20f))
    }

    @Test fun singlePointerThresholdRemainsStrictAndAlsoAppliesToReleaseEvent() {
        assertEquals(FeedImageTransform(1.01f, 0f, 0f, false), dynamicImageTransform(1.01f, 4f, 5f, 1, 1f, 3f, 2f))
        assertEquals(FeedImageTransform(1.0101f, 7f, 7f, true), dynamicImageTransform(1.0101f, 4f, 5f, 1, 1f, 3f, 2f))
        assertEquals(FeedImageTransform(2f, 7f, 7f, true), dynamicImageTransform(2f, 4f, 5f, 0, 1f, 3f, 2f))
    }

    @Test fun pinchJustAboveOneKeepsPanBeforeSinglePointerThreshold() {
        assertEquals(FeedImageTransform(1.005f, 3f, -2f, true), dynamicImageTransform(1f, 0f, 0f, 2, 1.005f, 3f, -2f))
    }
}
