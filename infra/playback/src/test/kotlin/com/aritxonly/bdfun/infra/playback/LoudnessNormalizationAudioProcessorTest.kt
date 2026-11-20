@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class LoudnessNormalizationAudioProcessorTest {
    private fun pcm16(count: Int, sample: Short): ByteBuffer =
        ByteBuffer.allocateDirect(count * 2).order(ByteOrder.nativeOrder()).apply {
            repeat(count) { putShort(sample) }
            flip()
        }

    private fun processor(encoding: Int) = LoudnessNormalizationAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(48_000, 1, encoding))
    }

    @Test fun quietInputIsBoostedAndFullScaleStaysClamped() {
        val processor = processor(C.ENCODING_PCM_16BIT)
        // 先用几块较安静的 PCM 让 AGC 增益升上来。
        repeat(8) {
            processor.queueInput(pcm16(480, 1_000))
            processor.getOutput().clear()
        }
        processor.queueInput(pcm16(480, Short.MAX_VALUE))
        val output = processor.getOutput()
        assertTrue(output.remaining() > 0)
        // 16-bit 输出必须削顶在 Short.MAX_VALUE，不能回绕成负值/爆音。
        assertEquals(Short.MAX_VALUE, output.getShort(output.position()))
    }

    @Test fun floatInputIsLimitedToUnity() {
        val processor = processor(C.ENCODING_PCM_FLOAT)
        repeat(8) {
            val quiet = ByteBuffer.allocateDirect(480 * 4).order(ByteOrder.nativeOrder()).apply {
                repeat(480) { putFloat(0.02f) }
                flip()
            }
            processor.queueInput(quiet)
            processor.getOutput().clear()
        }
        val loud = ByteBuffer.allocateDirect(4 * 4).order(ByteOrder.nativeOrder()).apply {
            repeat(4) { putFloat(0.99f) }
            flip()
        }
        processor.queueInput(loud)
        val output = processor.getOutput()
        assertEquals(1f, output.getFloat(output.position()), 0.0001f)
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun unsupportedEncodingIsRejected() {
        processor(C.ENCODING_PCM_8BIT)
    }
}
