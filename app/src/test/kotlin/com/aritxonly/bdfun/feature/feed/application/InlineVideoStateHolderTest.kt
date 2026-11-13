package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class InlineVideoStateHolderTest {
    private val info = PlayInfo(emptyList(), "audio", "fallback", 1000, emptyList())
    private val preferences = InlineVideoPreferences(64, "hevc", "base")
    private class Source : InlineVideoRepository {
        var cached: PlayInfo? = null
        var result: Outcome<PlayInfo> = Outcome.Failure(AppError.Network("offline"))
        var gate: CompletableDeferred<Unit>? = null
        val calls = mutableListOf<String>()
        val preferences = mutableListOf<InlineVideoPreferences>()
        override fun cachedPlayInfo(bvid: String) = cached
        override suspend fun load(bvid: String, preferences: () -> InlineVideoPreferences): Outcome<PlayInfo> {
            calls += bvid; gate?.await(); this.preferences += preferences(); return result
        }
    }

    @Test fun cachedInfoIsVisibleImmediatelyAndDoesNotRequestOrResampleSettings() = runBlocking {
        val source = Source(); source.cached = info
        val holder = InlineVideoStateHolder(source, "one") { error("缓存命中不能采样设置") }
        assertEquals(InlineVideoUiState(info), holder.state.value)
        holder.load(); assertTrue(source.calls.isEmpty())
    }

    @Test fun blankBvidKeepsCoverWithoutRequest() = runBlocking {
        val source = Source(); val holder = InlineVideoStateHolder(source, "  ") { preferences }
        holder.load(); assertTrue(source.calls.isEmpty()); assertEquals(InlineVideoUiState(), holder.state.value)
    }

    @Test fun successfulLoadPassesBvidAndCurrentSettings() = runBlocking {
        val source = Source(); source.result = Outcome.Success(info); source.gate = CompletableDeferred()
        var current = preferences
        val holder = InlineVideoStateHolder(source, "one") { current }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        current = InlineVideoPreferences(32, "avc", "other")
        source.gate!!.complete(Unit); job.join()
        assertEquals(listOf("one"), source.calls)
        assertEquals(listOf(current), source.preferences)
        assertEquals(InlineVideoUiState(info), holder.state.value)
    }

    @Test fun failedLoadKeepsCoverAndDoesNotRetryInSameController() = runBlocking {
        val source = Source(); val holder = InlineVideoStateHolder(source, "one") { preferences }
        holder.load(); holder.load()
        assertEquals(InlineVideoUiState(failed = true), holder.state.value)
        assertEquals(1, source.calls.size)
        source.result = Outcome.Success(info)
        val reentered = InlineVideoStateHolder(source, "one") { preferences }
        reentered.load(); assertEquals(InlineVideoUiState(info), reentered.state.value)
    }

    @Test fun pendingLoadIsNotDuplicatedAndCancellationDoesNotPoisonRetry() = runBlocking {
        val source = Source(); source.gate = CompletableDeferred()
        val holder = InlineVideoStateHolder(source, "one") { preferences }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        holder.load(); assertEquals(1, source.calls.size)
        job.cancelAndJoin(); assertTrue(job.isCancelled)
        assertEquals(InlineVideoUiState(), holder.state.value)
        source.gate = null; source.result = Outcome.Success(info); holder.load()
        assertEquals(2, source.calls.size); assertEquals(InlineVideoUiState(info), holder.state.value)
    }

    @Test fun closedPageRejectsLateSuccessOrFailureAndFutureLoads() = runBlocking {
        for (result in listOf(Outcome.Success(info), Outcome.Failure(AppError.Network("offline")))) {
            val source = Source(); source.result = result; source.gate = CompletableDeferred()
            val holder = InlineVideoStateHolder(source, "one") { preferences }
            val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
            holder.close(); source.gate!!.complete(Unit); job.join(); holder.load()
            assertEquals(InlineVideoUiState(), holder.state.value); assertEquals(1, source.calls.size)
        }
    }
}
