package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.Quality
import org.junit.Assert.*
import org.junit.Test

class AutomaticVideoQualitySelectorTest {
    private fun quality(id: Int, url: String = "$id") = Quality(id, "", 0, 0, "", "", url)
    private fun selector(kbps: Int, cellular: Boolean = false) =
        AutomaticVideoQualitySelector(NetworkBandwidthSource { kbps }, CellularNetworkSource { cellular })
    private fun select(kbps: Int, ids: List<Int>) =
        selector(kbps).select(ids.map { quality(it) })?.id
    private val ladder = listOf(127, 120, 116, 112, 80, 74, 64, 32, 16)

    @Test fun emptyListDoesNotReadPlatformBandwidth() {
        assertNull(AutomaticVideoQualitySelector(NetworkBandwidthSource { error("must not read") })
            .select(emptyList()))
    }
    @Test fun exactThresholdsUse1024Conversion() {
        val thresholds = listOf(40960 to 127, 24576 to 120, 10240 to 112,
            6144 to 80, 3072 to 64, 1536 to 32, 1 to 16, 0 to 64, -1 to 64)
        for ((kbps, expected) in thresholds) assertEquals("Kbps=$kbps", expected, select(kbps, ladder))
    }
    @Test fun valuesImmediatelyBelowThresholdUsePreviousTier() {
        for ((kbps, expected) in listOf(40959 to 120, 24575 to 112, 10239 to 80,
            6143 to 64, 3071 to 32, 1535 to 16)) assertEquals(expected, select(kbps, ladder))
    }
    @Test fun usesLadderOrderRatherThanInputOrderAndAllows116And74Fallback() {
        assertEquals(116, select(24576, listOf(16, 125, 116, 80, 126)))
        assertEquals(74, select(6144, listOf(16, 74, 64)))
    }
    @Test fun noHigherTierIsSelectedWhenLowerLadderCandidatesExist() {
        assertEquals(16, select(1, listOf(127, 80, 16)))
        assertEquals(32, select(3072, listOf(127, 80, 32)))
    }
    @Test fun absentLadderCandidateKeepsOriginalMinIdFallbackEvenForHdr() {
        assertEquals(125, select(0, listOf(126, 125)))
        assertEquals(127, select(1, listOf(127)))
        assertEquals(-1, select(40960, listOf(126, -1)))
    }
    @Test fun duplicateTierUsesFirstOriginalItem() {
        val first = quality(80, "first")
        assertSame(first, selector(6144).select(listOf(first, quality(80, "second"))))
    }
    @Test fun readsCurrentBandwidthOnEachSelection() {
        var kbps = 1
        val sut = AutomaticVideoQualitySelector(NetworkBandwidthSource { kbps })
        val items = ladder.map { quality(it) }
        assertEquals(16, sut.select(items)?.id)
        kbps = 40960
        assertEquals(127, sut.select(items)?.id)
    }
    @Test fun cellularCapsAutomaticTierAt1080p60() {
        val items = ladder.map { quality(it) }
        val mobile = selector(81920, cellular = true)
        assertEquals(116, mobile.select(items)?.id)
        // 没有 116 时退到 112，而不是 120/127。
        assertEquals(112, mobile.select(listOf(127, 120, 112, 80).map { quality(it) })?.id)
        // Wi-Fi 不受影响。
        val wifi = selector(81920, cellular = false)
        assertEquals(127, wifi.select(items)?.id)
    }
    @Test fun inlinePlaybackCapsQualityAt720p() {
        val items = ladder.map { quality(it) }
        assertEquals(64, capInlineQuality(items.first { it.id == 120 }, items)?.id)
        // 没有 720P 时取不高于 720P 的最高档。
        assertEquals(32, capInlineQuality(quality(120), listOf(quality(120), quality(32), quality(16)))?.id)
        // 本来就低于 720P 的不动。
        assertEquals(32, capInlineQuality(quality(32), items)?.id)
        assertNull(capInlineQuality(null, items))
    }
}
