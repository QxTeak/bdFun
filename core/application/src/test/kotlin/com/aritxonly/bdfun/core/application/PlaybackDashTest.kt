package com.aritxonly.bdfun.core.application

import com.aritxonly.bdfun.core.model.video.PlayInfo
import com.aritxonly.bdfun.core.model.video.Quality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackDashTest {
    private fun quality(id: Int, height: Int = 720, bandwidth: Long = 1_000_000L, url: String = "https://cdn/$id.m4s") =
        Quality(
            id = id, label = "$id", width = height * 16 / 9, height = height,
            frameRate = "30", codec = "avc1.640028", videoUrl = url,
            bandwidth = bandwidth, initRange = "0-999", indexRange = "1000-1999",
        )

    private fun playInfo(
        qualities: List<Quality> = listOf(quality(80, 1080), quality(64, 720)),
        audioInit: String? = "0-836",
    ) = PlayInfo(
        qualities = qualities,
        audioUrl = "https://cdn/audio.m4s?x=1&y=2",
        fallbackUrl = null,
        durationMs = 120_500L,
        acceptQuality = listOf(80, 64),
        audioBandwidth = 118_299L,
        audioCodec = "mp4a.40.2",
        audioInitRange = audioInit,
        audioIndexRange = "837-1116",
    )

    @Test fun adaptiveRequiresCompleteByteRangesOnVideoAndAudio() {
        assertTrue(dashAdaptiveAvailable(playInfo()))
        assertFalse(dashAdaptiveAvailable(playInfo(audioInit = null)))
        assertFalse(dashAdaptiveAvailable(playInfo(qualities = listOf(quality(80), quality(64).copy(indexRange = null)))))
        assertFalse(dashAdaptiveAvailable(playInfo(qualities = listOf(quality(80)))))
        assertFalse(dashAdaptiveAvailable(playInfo().copy(durationMs = 0L)))
    }

    @Test fun manifestCarriesEveryQualityAndEscapesUrls() {
        val dash = buildPlaybackDashSource(playInfo(), qualityId = 64)
        assertNotNull(dash)
        val manifest = dash!!.manifest
        assertTrue(manifest.contains("<Representation id=\"80\" bandwidth=\"1000000\""))
        assertTrue(manifest.contains("<Representation id=\"64\" bandwidth=\"1000000\""))
        assertTrue(manifest.contains("<Initialization range=\"0-999\"/>"))
        assertTrue(manifest.contains("indexRange=\"1000-1999\""))
        assertTrue(manifest.contains("mediaPresentationDuration=\"PT120.500S\""))
        assertTrue(manifest.contains("<AdaptationSet contentType=\"audio\""))
        // URL 里的 & 必须转义，否则 MPD 解析失败。
        assertTrue(manifest.contains("https://cdn/audio.m4s?x=1&amp;y=2"))
        assertEquals(64, dash.videoQualityId)
        assertEquals(listOf(80, 64), dash.videoTracks.map { it.id })
    }

    @Test fun unknownQualityOrDecimalFrameRateIsHandledWithoutBreakingTheManifest() {
        assertNull(buildPlaybackDashSource(playInfo(), qualityId = 32))
        // 小数帧率写进 MPD 会让解析失败：应该整段省略，而不是原样写。
        val dash = buildPlaybackDashSource(
            playInfo(qualities = listOf(quality(80).copy(frameRate = "23.986"), quality(64))),
            qualityId = 80,
        )!!
        assertFalse(dash.manifest.contains("frameRate=\"23.986\""))
        assertTrue(dash.manifest.contains("<Representation id=\"64\""))
    }
}
