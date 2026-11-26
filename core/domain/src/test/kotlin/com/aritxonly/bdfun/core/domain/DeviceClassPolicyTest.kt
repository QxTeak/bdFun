package com.aritxonly.bdfun.core.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceClassPolicyTest {
    @Test fun compactPhonesAreNotTablets() {
        assertFalse(isTabletDevice(0))
        assertFalse(isTabletDevice(320))
        assertFalse(isTabletDevice(393))
        assertFalse(isTabletDevice(599))
    }

    @Test fun sw600AndAboveAreTablets() {
        assertTrue(isTabletDevice(600))
        assertTrue(isTabletDevice(840))
        assertTrue(isTabletDevice(1280))
    }
}
