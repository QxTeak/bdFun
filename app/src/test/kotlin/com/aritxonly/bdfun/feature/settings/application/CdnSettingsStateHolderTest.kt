package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.CdnSettingsSource
import com.aritxonly.bdfun.core.model.playback.CdnService
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale

class CdnSettingsStateHolderTest {
    private class Fake : CdnSettingsSource {
        override val services = listOf(CdnService("auto", "自动", auto = true), CdnService("base", "原地址"), CdnService("ali", "节点"))
        var map: Map<String, Double> = emptyMap()
        var current: String? = "ali"
        var fresh = false
        var probing = false
        val forces = mutableListOf<Boolean>()
        var hook: suspend () -> Unit = {}
        var reads = 0
        override fun speeds(): Map<String, Double> { reads++; return map }
        override fun currentServiceId() = current
        override fun isFresh() = fresh
        override fun isProbing() = probing
        override suspend fun refresh(force: Boolean): Outcome<Boolean> { forces += force; hook(); return Outcome.Success(true) }
    }
    @Test fun labelsKeepServiceOrderAutoResolutionStrictThresholdAndUsDecimalFormat() {
        val old = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val f = Fake()
            assertEquals(listOf("自动  1.3 MB/s", "原地址", "节点  1.3 MB/s"), cdnMenuLabels(f.services,
                mapOf("ali" to 1.25, "base" to 0.01), "ali"))
            assertEquals(listOf("自动", "原地址", "节点"), cdnMenuLabels(f.services,
                mapOf("auto" to 9.0, "ali" to Double.NaN, "base" to -1.0), null))
        } finally { Locale.setDefault(old) }
    }
    @Test fun initialCachedSpeedsDisplayImmediatelyAndLoadNeverProbes() = runBlocking {
        val f = Fake().apply { map = mapOf("ali" to 2.0); fresh = true }
        val h = CdnSettingsStateHolder(f)
        assertEquals(listOf("自动  2.0 MB/s", "原地址", "节点  2.0 MB/s"), h.state.value.labels)
        h.load()
        assertEquals(0, f.forces.size)
        assertEquals(0, h.selectedIndex("missing")); assertEquals(0, h.selectedIndex("auto")); assertEquals(2, h.selectedIndex("ali"))
    }
    @Test fun staleCacheIsNotProbedUntilTheUserOpensTheSelector() = runBlocking {
        val f = Fake().apply { map = mapOf("ali" to 1.0) }
        f.hook = { f.map = mapOf("ali" to 3.0) }
        val h = CdnSettingsStateHolder(f)
        h.load()
        assertEquals(0, f.forces.size)
        assertEquals("节点  1.0 MB/s", h.state.value.labels.last())
        h.probe()
        assertEquals(listOf(false), f.forces)
        assertEquals("自动  3.0 MB/s", h.state.value.labels.first())
    }
    @Test fun freshCacheSkipsProbeWhenTheSelectorOpens() = runBlocking {
        val f = Fake().apply { fresh = true; map = mapOf("ali" to 4.0) }
        val h = CdnSettingsStateHolder(f)
        h.probe()
        assertTrue(f.forces.isEmpty())
        assertEquals("节点  4.0 MB/s", h.state.value.labels.last())
    }
    @Test fun closeRejectsLateProbeSnapshotAndFutureRequests() = runBlocking {
        val f = Fake(); val gate = CompletableDeferred<Unit>(); f.hook = { gate.await(); f.map = mapOf("ali" to 9.0) }
        val h = CdnSettingsStateHolder(f); val initial = h.state.value
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.probe() }
        h.close(); gate.complete(Unit); job.join(); val reads = f.reads; h.load()
        assertTrue(job.isCancelled); assertEquals(initial, h.state.value); assertEquals(reads, f.reads); assertEquals(1, f.forces.size)
    }
    @Test fun duplicateProbeIsIgnoredWhileOneIsRunning() = runBlocking {
        val f = Fake(); val gate = CompletableDeferred<Unit>()
        f.hook = { gate.await(); f.map = mapOf("ali" to 2.0) }
        val h = CdnSettingsStateHolder(f)
        val first = launch(start = CoroutineStart.UNDISPATCHED) { h.probe() }
        h.probe()
        assertEquals(1, f.forces.size)
        gate.complete(Unit); first.join()
        assertEquals("节点  2.0 MB/s", h.state.value.labels.last())
    }
    @Test fun probePollsUntilTheBackgroundProbeFinishes() = runBlocking {
        val f = Fake().apply { probing = true }
        val h = CdnSettingsStateHolder(f)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { h.probe() }
        f.map = mapOf("ali" to 5.0)
        f.probing = false
        job.join()
        assertEquals("节点  5.0 MB/s", h.state.value.labels.last())
    }
}
