package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.playbackCodecPriority
import com.aritxonly.bdfun.core.domain.playbackCodecMatches
import com.aritxonly.bdfun.core.domain.choosePlaybackVideo
import com.aritxonly.bdfun.core.domain.choosePlaybackAudio
import com.aritxonly.bdfun.core.domain.isDolbyVisionQuality
import com.aritxonly.bdfun.core.model.video.Quality
import org.junit.Assert.*
import org.junit.Test

class PlaybackPolicyTest {
    private data class Track(val id: Int, val codec: String, val bandwidth: Long)
    private fun video(items: List<Track>, preference: String = "auto") =
        choosePlaybackVideo(items, preference, Track::codec, Track::bandwidth)

    @Test fun autoPrefersAvcOverLargerAv1() {
        val avc = Track(80, "avc1", 100)
        assertSame(avc, video(listOf(Track(80, "av01", 900), avc)))
    }
    @Test fun autoUsesHighestBandwidthWithinCompatibleCodec() {
        val best = Track(80, "avc1", 200)
        assertSame(best, video(listOf(Track(80, "avc1", 100), best)))
    }
    @Test fun explicitHevcSelectsHighestMatchingRepresentation() {
        val best = Track(80, "hvc1", 500)
        assertSame(best, video(listOf(Track(80, "avc1", 900), Track(80, "hev1", 200), best), "hevc"))
    }
    @Test fun unavailablePreferenceFallsBackToAvc() {
        val avc = Track(80, "avc1", 100)
        assertSame(avc, video(listOf(Track(80, "av01", 900), avc), "hevc"))
    }
    @Test fun hevcBeatsAv1WhenAvcIsAbsent() {
        val hevc = Track(120, "hvc1", 100)
        assertSame(hevc, video(listOf(Track(120, "av01", 900), hevc)))
    }
    @Test fun standardAudioIdWinsOverExoticHigherBandwidth() {
        val standard = Track(30280, "", 100)
        val selected = choosePlaybackAudio(listOf(Track(30251, "", 999), Track(30232, "", 90), standard), Track::id, Track::bandwidth)
        assertSame(standard, selected)
    }
    @Test fun unknownAudioFallsBackToSmallestBandwidthAndEmptyListsStayEmpty() {
        val small = Track(1, "", 10)
        assertSame(small, choosePlaybackAudio(listOf(Track(2, "", 100), small), Track::id, Track::bandwidth))
        assertNull(video(emptyList()))
        assertNull(choosePlaybackAudio(emptyList<Track>(), Track::id, Track::bandwidth))
    }
    @Test fun codecAliasesAndDolbyKeepLegacySemantics() {
        assertEquals(0, playbackCodecPriority("H264"))
        assertEquals(1, playbackCodecPriority("h265"))
        assertEquals(2, playbackCodecPriority("AV01"))
        assertEquals(3, playbackCodecPriority("unknown"))
        assertTrue(playbackCodecMatches("dvhe.05", "dvh1"))
        assertFalse(playbackCodecMatches("h264", "avc"))
    }
    @Test fun dolbyVisionQualityDetectedByIdAndCodecOnly() {
        // B 站杜比视界：id 126，codecs 可能是 dvh1.xx / dvhe.xx。
        assertTrue(isDolbyVisionQuality(quality(126, "hev1.2.4")))
        assertTrue(isDolbyVisionQuality(quality(120, "dvh1.05.06")))
        assertTrue(isDolbyVisionQuality(quality(80, "dvhe.08.06")))
        // HDR10 / 普通 HEVC / AV1 仍走原来的纹理表面，不能误判。
        assertFalse(isDolbyVisionQuality(quality(125, "hev1.2.4")))
        assertFalse(isDolbyVisionQuality(quality(120, "hev1.2.4")))
        assertFalse(isDolbyVisionQuality(quality(80, "avc1.640028")))
        assertFalse(isDolbyVisionQuality(null))
    }
    private fun quality(id: Int, codec: String) = Quality(id, "", 0, 0, "", codec, "url")
}
