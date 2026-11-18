package com.aritxonly.bdfun.infra.playback

import com.aritxonly.bdfun.core.domain.buffering.BufferHistoryStore
import com.aritxonly.bdfun.core.domain.buffering.BufferSession
import com.aritxonly.bdfun.core.domain.buffering.WatchHistory
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
class StrategyLoadControlTest {
    private fun session(enabled: Boolean): BufferSession {
        val store = object : BufferHistoryStore {
            override fun read(account: String) = WatchHistory()
            override fun observe(account: String, watchedMs: Long, abandoned: Boolean) = Unit
        }
        return BufferSession(store).also { it.configure("test", enabled) }
    }

    @Test fun fixedModeConstructsRealMedia3LoadControl() {
        assertNotNull(StrategyLoadControl(session(false)).allocator)
    }

    @Test fun adaptiveModeConstructsRealMedia3LoadControl() {
        assertNotNull(StrategyLoadControl(session(true)).allocator)
    }
}
