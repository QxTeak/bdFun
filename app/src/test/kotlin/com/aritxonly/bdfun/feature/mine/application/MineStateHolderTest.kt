package com.aritxonly.bdfun.feature.mine.application

import com.aritxonly.bdfun.core.model.user.UserProfileStats

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.MineRepository
import com.aritxonly.bdfun.core.model.user.UserProfile
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MineStateHolderTest {
    private fun profile(mid: Long = 1) = UserProfile(
        mid = mid,
        name = "old",
        face = "face",
        sign = "sign",
        level = 3,
        coins = 2.5,
        vip = false,
        vipLabel = "",
        stats = UserProfileStats(
            follower = 20,
            following = 10,
            likes = 30,
        ),
    )
    private class Fake(val initial: UserProfile) : MineRepository {
        val calls = mutableListOf<String>()
        var nav: Outcome<UserProfile?> = Outcome.Success(initial.copy(
            name = "fresh",
            stats = initial.stats.copy(
                following = 999,
                follower = 998,
                likes = 997,
            ),
        ))
        var relation: Outcome<Pair<Long, Long>> = Outcome.Success(40L to 50L)
        var liked: Outcome<Long> = Outcome.Success(60L)
        var hook: suspend (String) -> Unit = {}
        private suspend fun hit(key: String) { calls += key; hook(key) }
        override suspend fun profile(): Outcome<UserProfile?> { hit("nav"); return nav }
        override suspend fun relationStat(mid: Long): Outcome<Pair<Long, Long>> { hit("relation:$mid"); return relation }
        override suspend fun likes(mid: Long): Outcome<Long> { hit("likes:$mid"); return liked }
    }
    private val failure = Outcome.Failure(AppError.Network("offline"))

    @Test fun guestDoesNotRequestOrLoadCache() = runBlocking {
        val fake = Fake(profile()); val h = MineStateHolder(null, fake, MineProfileStore { 0 })
        h.load(); assertEquals(MineUiState(null), h.state.value); assertTrue(fake.calls.isEmpty())
    }
    @Test fun requestsKeepOriginalOrderAndFieldPrecedence() = runBlocking {
        val p = profile(); val fake = Fake(p); val h = MineStateHolder(p, fake, MineProfileStore { 0 })
        h.load(); assertEquals(listOf("nav", "relation:1", "likes:1"), fake.calls)
        assertEquals(p.copy(
            name = "fresh",
            stats = p.stats.copy(
                following = 40,
                follower = 50,
                likes = 60,
            ),
        ), h.state.value.profile)
        assertFalse(h.state.value.loading)
    }
    @Test fun eachFailureFallsBackToInitialFieldsEvenWhenNavContainsOtherCounts() = runBlocking {
        val p = profile()
        for (mask in 0..7) {
            val fake = Fake(p)
            if (mask and 1 != 0) fake.nav = failure
            if (mask and 2 != 0) fake.relation = failure
            if (mask and 4 != 0) fake.liked = failure
            val h = MineStateHolder(p, fake, MineProfileStore { 0 }); h.load()
            assertEquals(p.copy(
                name = if (mask and 1 != 0) "old" else "fresh",
                stats = p.stats.copy(
                    following = if (mask and 2 != 0) 10 else 40,
                    follower = if (mask and 2 != 0) 20 else 50,
                    likes = if (mask and 4 != 0) 30 else 60,
                ),
            ), h.state.value.profile)
            assertEquals(3, fake.calls.size)
        }
    }
    @Test fun successfulNullNavRetainsInitialProfileAndLoadsOtherCounts() = runBlocking {
        val p = profile(); val fake = Fake(p).apply { nav = Outcome.Success(null) }
        val h = MineStateHolder(p, fake, MineProfileStore { 0 }); h.load()
        assertEquals(p.copy(
            stats = p.stats.copy(
                following = 40,
                follower = 50,
                likes = 60,
            ),
        ), h.state.value.profile)
    }
    @Test fun cacheReusesMergedResultAndExpiresAtExactlyFiveMinutesFromRequestStart() = runBlocking {
        var now = 1000L; val p = profile(); val fake = Fake(p); val cache = MineProfileStore { now }
        fake.hook = { if (it == "likes:1") now += 2000 }
        val first = MineStateHolder(p, fake, cache); first.load()
        now = 300999; val second = MineStateHolder(p.copy(name = "input changed"), fake, cache); second.load()
        assertEquals(first.state.value.profile, second.state.value.profile); assertEquals(3, fake.calls.size)
        now = 301000; second.load(); assertEquals(6, fake.calls.size)
    }
    @Test fun cacheRemainsSingleOwnerAndCanBeCleared() {
        val cache = MineProfileStore { 10 }; val p = profile()
        cache.put(1, p.copy(mid = 9), 0)
        assertNotNull(cache.cached(1)); assertNull(cache.cached(9))
        cache.put(2, profile(2), 0); assertNull(cache.cached(1)); assertNotNull(cache.cached(2))
        cache.clear(); assertNull(cache.cached(2))
    }
    @Test fun duplicateLoadWhileBusyDoesNotStartAnotherRequest() = runBlocking {
        val fake = Fake(profile()); val gate = CompletableDeferred<Unit>()
        fake.hook = { if (it == "nav") gate.await() }
        val h = MineStateHolder(profile(), fake, MineProfileStore { 0 })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.load() }
        h.load(); assertEquals(listOf("nav"), fake.calls)
        gate.complete(Unit); job.join(); assertEquals(3, fake.calls.size)
    }
    @Test fun closeRejectsEveryLateStageAndDoesNotPopulateProcessCache() = runBlocking {
        for (stage in listOf("nav", "relation:1", "likes:1")) {
            val p = profile(); val fake = Fake(p); val cache = MineProfileStore { 0 }
            val gate = CompletableDeferred<Unit>(); fake.hook = { if (it == stage) gate.await() }
            val h = MineStateHolder(p, fake, cache)
            val job = launch(start = CoroutineStart.UNDISPATCHED) { h.load() }
            h.close(); val count = fake.calls.size; gate.complete(Unit); job.join(); h.load()
            assertTrue(job.isCancelled); assertEquals(count, fake.calls.size)
            assertEquals(MineUiState(p), h.state.value); assertNull(cache.cached(1))
        }
    }
    @Test fun ownerInvalidationStopsBeforeNextReadAndDoesNotPublishStaleProfile() = runBlocking {
        var owner = 1L; val p = profile(); val fake = Fake(p); val cache = MineProfileStore { 0 }
        val gate = CompletableDeferred<Unit>(); fake.hook = { if (it == "relation:1") gate.await() }
        val h = MineStateHolder(p, fake, cache) { owner == 1L }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.load() }
        owner = 2; gate.complete(Unit); job.join(); h.load()
        assertTrue(job.isCancelled); assertEquals(listOf("nav", "relation:1"), fake.calls)
        assertEquals(MineUiState(p), h.state.value); assertNull(cache.cached(1))
    }
    @Test fun swallowedCancellationDoesNotContinueAndRetryStillWorks() = runBlocking {
        val p = profile(); val fake = Fake(p); val h = MineStateHolder(p, fake, MineProfileStore { 0 })
        fake.hook = { try { awaitCancellation() } catch (_: CancellationException) {} }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.load() }; job.cancelAndJoin()
        assertEquals(listOf("nav"), fake.calls); assertEquals(MineUiState(p), h.state.value)
        fake.hook = {}; h.load(); assertEquals(4, fake.calls.size)
    }
    @Test fun recordTimeFormattingKeepsOriginalNegativeMinuteAndHourRules() {
        assertEquals("00:00", formatVideoPosition(-1))
        assertEquals("00:59", formatVideoPosition(59999))
        assertEquals("01:00", formatVideoPosition(60000))
        assertEquals("1:00:01", formatVideoPosition(3601000))
    }

    @Test fun expiredCacheIsInitialStateAndFailedRefreshKeepsItsFields() = runBlocking {
        val p = profile()
        val cached = p.copy(name = "cached", face = "cached-face")
        val cache = MineProfileStore { 400000L }
        cache.put(p.mid, cached, 0)
        val fake = Fake(p).apply { nav = failure; relation = failure; liked = failure }
        val holder = MineStateHolder(p, fake, cache)
        assertEquals(cached, holder.state.value.profile)
        holder.load()
        assertEquals(cached, holder.state.value.profile)
        assertFalse(holder.state.value.loading)
    }
}
