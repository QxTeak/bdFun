package com.aritxonly.bdfun.core.domain.buffering

import org.junit.Assert.*
import org.junit.Test

class BufferSessionTest {
    private class Store : BufferHistoryStore {
        val records = mutableListOf<Triple<String, Long, Boolean>>()
        override fun read(account: String) = WatchHistory()
        override fun observe(account: String, watchedMs: Long, abandoned: Boolean) {
            records += Triple(account, watchedMs, abandoned)
        }
    }

    @Test fun pauseAndSameMediaReprepareDoNotEndOrInflateSession() {
        val store = Store()
        val session = BufferSession(store)
        session.configure("one", true)
        session.begin("video")
        session.sample(0, true, false, false)
        session.sample(2_000, false, false, false)
        session.begin("video")
        session.sample(20_000, true, false, false)
        session.sample(23_000, false, false, false)
        session.finish()
        session.finish()
        assertEquals(listOf(Triple("one", 5_000L, true)), store.records)
    }

    @Test fun endBufferingAndErrorsAreCensored() {
        repeat(3) { reason ->
            val store = Store()
            val session = BufferSession(store)
            session.configure("one", true)
            session.begin("video")
            session.sample(0, true, false, false)
            session.sample(5_000, false, reason == 1, reason == 0)
            if (reason == 2) session.invalidate()
            session.finish()
            assertEquals(listOf(Triple("one", 5_000L, false)), store.records)
        }
    }

    @Test fun accountChangeCannotAttributeOldWatchTimeToNewOwner() {
        val store = Store()
        val session = BufferSession(store)
        session.configure("one", true)
        session.begin("video")
        session.sample(0, true, false, false)
        session.sample(5_000, true, false, false)
        session.configure("two", true)
        session.sample(5_000, true, false, false)
        session.sample(7_000, false, false, false)
        session.finish()
        assertEquals(listOf(Triple("two", 2_000L, false)), store.records)
    }

    @Test fun disablingDropsPendingSampleAndDoesNotTrain() {
        val store = Store()
        val session = BufferSession(store)
        session.configure("one", true)
        session.begin("video")
        session.sample(0, true, false, false)
        session.sample(5_000, true, false, false)
        session.configure("one", false)
        session.sample(6_000, true, false, false)
        session.sample(9_000, false, false, false)
        session.finish()
        assertTrue(store.records.isEmpty())
    }
}
