package com.aritxonly.teak.common.navigation

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class AdaptiveTabGestureTest {
    @Test fun draggingUsesContinuousPositionAndClampsBothEdges() {
        assertEquals(2.25f, draggedTabPosition(2f, -12.5f, 50f, 5), .0001f)
        assertEquals(0f, draggedTabPosition(0f, 100f, 50f, 5), .0001f)
        assertEquals(4f, draggedTabPosition(4f, -100f, 50f, 5), .0001f)
    }

    @Test fun slowReleaseSnapsNearestAndFastReleaseAdvancesAtMostOne() {
        assertEquals(2, settledTabIndex(2.4f, 0f, 50f, 5))
        assertEquals(3, settledTabIndex(2.6f, 0f, 50f, 5))
        assertEquals(3, settledTabIndex(2f, -10000f, 50f, 5))
        assertEquals(1, settledTabIndex(2f, 10000f, 50f, 5))
        assertEquals(0, settledTabIndex(0f, 10000f, 50f, 5))
        assertEquals(4, settledTabIndex(4f, -10000f, 50f, 5))
    }

    @Test fun newDragRetainsOwnershipWhenPreviousPageWaitIsCancelled() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val motion = AdaptiveTabMotion()
            var commits = 0
            motion.select(3, { 0f }, scope, false) { commits++ }
            assertEquals(3f, motion.value(0f), .0001f)
            motion.begin(0f)
            motion.drag(-25f, 50f, 5)
            assertEquals(3.5f, motion.value(0f), .0001f)
            assertEquals(1, commits)
            assertEquals(4, motion.end(0f, 50f, 5))
            motion.select(4, { 4f }, scope, false) { commits++ }
            assertEquals(4f, motion.value(4f), .0001f)
            assertEquals(2, commits)
        } finally { scope.cancel() }
    }
}
