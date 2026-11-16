package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.domain.RecommendationStorage
import com.aritxonly.bdfun.core.model.feed.RecommendationArchive
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class RecommendationOptimizerTest {
    private class Storage : RecommendationStorage {
        var archive = RecommendationArchive()
        override fun read() = archive
        override fun write(archive: RecommendationArchive) { this.archive = archive }
    }
    private val video = VideoItem("BV1", 1, "", "", "", "", 1, 0, 0, 60, 0)
    private suspend fun ready(optimizer: RecommendationOptimizer) {
        withTimeout(1000) { while (!optimizer.state.value.ready) yield() }
    }
    @Test fun defaultOffDoesNotLearnAndLeavesOrderUntouched() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { "a" to 0L }, { 1000L }, Dispatchers.Unconfined)
            ready(optimizer)
            val ticket = optimizer.ticket()
            optimizer.expose(video, ticket); optimizer.watch(video, ticket, 1.0)
            assertNull(optimizer.open(video))
            assertTrue(storage.archive.accounts.isEmpty())
            assertEquals(listOf(video), optimizer.rank(listOf(video), emptyList(), ticket))
        } finally { scope.cancel() }
    }
    @Test fun clearRejectsOldEventsAndKeepsTheSwitch() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { "a" to 0L }, { 1000L }, Dispatchers.Unconfined)
            ready(optimizer); optimizer.setEnabled(true)
            val ticket = optimizer.open(video)!!
            optimizer.watch(video, ticket, 1.0)
            assertFalse(storage.archive.accounts.isEmpty())
            optimizer.clear(); optimizer.watch(video, ticket, 1.0); optimizer.expose(video, ticket)
            assertTrue(storage.archive.accounts.isEmpty())
            assertTrue(storage.archive.enabled)
            assertFalse(optimizer.valid(ticket))
        } finally { scope.cancel() }
    }
    @Test fun modeRoundTripRejectsOldTicketAndRetainsMemory() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { "a" to 0L }, { 1000L }, Dispatchers.Unconfined)
            ready(optimizer); optimizer.setEnabled(true)
            val ticket = optimizer.open(video)!!
            optimizer.setEnabled(false); optimizer.setEnabled(true)
            assertFalse(optimizer.valid(ticket))
            assertNull(optimizer.playbackTicket(video.bvid))
            assertEquals(1, storage.archive.accounts["a"]?.size)
        } finally { scope.cancel() }
    }
    @Test fun accountReplacementRejectsLatePlaybackEvenBeforeCollectorRuns() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var owner = "a" to 0L
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { owner }, { 1000L }, Dispatchers.Unconfined)
            ready(optimizer); optimizer.setEnabled(true)
            val old = optimizer.open(video)!!
            owner = "b" to 1L
            optimizer.watch(video, old, 1.0)
            assertNull(storage.archive.accounts["b"])
            optimizer.accountChanged()
            assertNull(optimizer.playbackTicket(video.bvid))
            val next = optimizer.open(video)!!
            optimizer.watch(video, next, 0.5)
            assertEquals(0.0, storage.archive.accounts["a"]!!.single().satisfaction, 0.0)
            assertEquals(0.5, storage.archive.accounts["b"]!!.single().satisfaction, 0.0)
        } finally { scope.cancel() }
    }
    @Test fun guestMemoryNeverPersistsOrMovesIntoAnAccount() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var owner = "" to 0L
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { owner }, { 1000L }, Dispatchers.Unconfined)
            ready(optimizer); optimizer.setEnabled(true)
            val old = optimizer.open(video)!!
            optimizer.watch(video, old, 1.0)
            assertTrue(storage.archive.accounts.isEmpty())
            owner = "a" to 1L; optimizer.accountChanged()
            assertNull(optimizer.playbackTicket(video.bvid))
            assertTrue(storage.archive.accounts.isEmpty())
        } finally { scope.cancel() }
    }
    @Test fun exposureIsIdempotentAndCannotRefreshLearnedInterest() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            var now = 1000L
            val storage = Storage()
            val optimizer = RecommendationOptimizer(storage, scope, { "a" to 0L }, { now }, Dispatchers.Unconfined)
            ready(optimizer); optimizer.setEnabled(true)
            val ticket = optimizer.open(video)!!
            optimizer.watch(video, ticket, 1.0)
            now = 2000; optimizer.expose(video, ticket)
            now = 3000; optimizer.expose(video, ticket)
            val row = storage.archive.accounts["a"]!!.single()
            assertEquals(2000L, row.exposedAt)
            assertEquals(1000L, row.watchedAt)
        } finally { scope.cancel() }
    }
}
