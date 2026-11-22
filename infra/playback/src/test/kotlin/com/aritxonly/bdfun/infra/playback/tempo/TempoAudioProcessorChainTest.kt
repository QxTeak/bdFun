@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback.tempo

import androidx.media3.common.C
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoAudioProcessorChainTest {

    private fun tempo(speed: Float = 1f): TempoAudioProcessor = TempoAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(48_000, 2, C.ENCODING_PCM_16BIT))
        flush()
        this.speed = speed
    }

    @Test
    fun playbackParametersReachTheProcessorAndPitchIsLocked() {
        val processor = tempo()
        val chain = TempoAudioProcessorChain(arrayOf(processor), processor)
        val applied = chain.applyPlaybackParameters(PlaybackParameters(2.5f, 1.3f))
        assertEquals(2.5f, processor.speed, 1e-6f)
        // 自研处理器只做时长伸缩，音高恒为 1，不能把调用方传的 pitch 透传下去。
        assertEquals(2.5f, applied.speed, 1e-6f)
        assertEquals(1f, applied.pitch, 1e-6f)
    }

    @Test
    fun mediaDurationScalesWithSpeedOnlyWhenActive() {
        val processor = tempo()
        val chain = TempoAudioProcessorChain(arrayOf(processor), processor)

        chain.applyPlaybackParameters(PlaybackParameters(1f, 1f))
        assertEquals(1_000_000L, chain.getMediaDuration(1_000_000L))

        chain.applyPlaybackParameters(PlaybackParameters(2f, 1f))
        assertEquals(2_000_000L, chain.getMediaDuration(1_000_000L))

        chain.applyPlaybackParameters(PlaybackParameters(0.5f, 1f))
        assertEquals(500_000L, chain.getMediaDuration(1_000_000L))
    }

    @Test
    fun skipSilenceStaysOffAndNothingIsReportedSkipped() {
        val processor = tempo(speed = 2f)
        val chain = TempoAudioProcessorChain(arrayOf(processor), processor)
        assertFalse(chain.applySkipSilenceEnabled(true))
        assertEquals(0L, chain.getSkippedOutputFrameCount())
        assertEquals(1, chain.getAudioProcessors().size)
        assertTrue(chain.getAudioProcessors().first() === processor)
    }
}
