package com.aritxonly.bdfun.core.domain

import org.junit.Assert.*
import org.junit.Test

class TabClickPolicyTest {
    @Test fun returnOnlyRefreshesOnlyAtTop() {
        assertFalse(TabClickPolicy.DEFAULT_REFRESH)
        assertFalse(TabClickPolicy.shouldRefresh(false, false))
        assertTrue(TabClickPolicy.shouldRefresh(false, true))
    }
    @Test fun refreshModeRefreshesAtEitherPosition() {
        assertTrue(TabClickPolicy.shouldRefresh(true, false))
        assertTrue(TabClickPolicy.shouldRefresh(true, true))
    }
}
