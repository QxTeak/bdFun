@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback.tempo

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

class TempoAudioProcessorTest {

    private val sampleRate = 48_000

    private fun pcm16(frames: Int, channels: Int, frequency: Double = 440.0): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frames) {
            val value = (0.4 * sin(2.0 * PI * frequency * frame / sampleRate) * 32_767).toInt()
            repeat(channels) { buffer.putShort(value.toShort()) }
        }
        buffer.flip()
        return buffer
    }

    private fun TempoAudioProcessor.drainInto(target: ByteArrayOutputStream) {
        while (true) {
            val output = getOutput()
            if (!output.hasRemaining()) break
            val chunk = ByteArray(output.remaining())
            output.get(chunk)
            target.write(chunk)
        }
    }

    private fun processor(
        encoding: Int = C.ENCODING_PCM_16BIT,
        channels: Int = 2,
        speed: Float = 1f,
    ): TempoAudioProcessor = TempoAudioProcessor().apply {
        configure(AudioProcessor.AudioFormat(sampleRate, channels, encoding))
        flush()
        this.speed = speed
    }

    @Test
    fun unitySpeedIsInactiveSoMedia3BypassesIt() {
        val processor = processor(speed = 1f)
        assertFalse(processor.isActive())
        processor.speed = 1.05f
        assertTrue(processor.isActive())
        processor.speed = 2f
        assertTrue(processor.isActive())
    }

    @Test
    fun twoTimesHalvesOutputFrames() {
        val processor = processor(speed = 2f)
        val input = pcm16(sampleRate, channels = 2)
        processor.queueInput(input)
        assertEquals("必须吃干净输入", 0, input.remaining())

        val output = ByteArrayOutputStream()
        processor.drainInto(output)
        processor.queueEndOfStream()
        processor.drainInto(output)

        val outputFrames = output.size() / (2 * 2)
        assertEquals(sampleRate / 2.0, outputFrames.toDouble(), sampleRate * 0.06)
        assertTrue(processor.isEnded())
    }

    @Test
    fun floatEncodingKeepsFloatOutput() {
        val processor = processor(encoding = C.ENCODING_PCM_FLOAT, channels = 1, speed = 2f)
        val frames = 24_000
        val input = ByteBuffer.allocateDirect(frames * 4).order(ByteOrder.LITTLE_ENDIAN).apply {
            for (frame in 0 until frames) {
                putFloat((0.3 * sin(2.0 * PI * 300.0 * frame / sampleRate)).toFloat())
            }
            flip()
        }
        processor.queueInput(input)
        val output = ByteArrayOutputStream()
        processor.drainInto(output)
        processor.queueEndOfStream()
        processor.drainInto(output)
        val floats = output.size() / 4
        assertEquals(12_000.0, floats.toDouble(), 12_000 * 0.08)
    }

    @Test
    fun durationMappingFollowsSpeed() {
        val processor = processor(speed = 2f)
        assertEquals(500_000L, processor.getDurationAfterProcessorApplied(1_000_000L))
        processor.speed = 1f
        assertEquals(1_000_000L, processor.getDurationAfterProcessorApplied(1_000_000L))
    }

    @Test(expected = AudioProcessor.UnhandledAudioFormatException::class)
    fun unsupportedEncodingIsRejected() {
        processor(encoding = C.ENCODING_PCM_8BIT)
    }

    @Test
    fun flushDropsBufferedAudio() {
        val processor = processor(speed = 2f)
        processor.queueInput(pcm16(8_192, channels = 2))
        processor.drainInto(ByteArrayOutputStream())
        processor.flush()
        assertFalse(processor.getOutput().hasRemaining())
        assertFalse(processor.isEnded())
    }

    @Test
    fun outputBytesStayAlignedToFrames() {
        val processor = processor(speed = 1.5f, channels = 2)
        val output = ByteArrayOutputStream()
        processor.queueInput(pcm16(48_000, channels = 2))
        processor.drainInto(output)
        processor.queueEndOfStream()
        processor.drainInto(output)
        assertEquals(0, output.size() % 4)
        val samples = output.toByteArray()
        val buffer = ByteBuffer.wrap(samples)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        var peak = 0
        for (index in 0 until samples.size / 2) {
            val value = kotlin.math.abs(buffer.getShort(index * 2).toInt())
            if (value > peak) peak = value
        }
        assertTrue("peak=$peak", peak <= 32_767)
        assertTrue("peak=$peak", peak > 1_000)
    }

    @Test
    fun resetClearsEngine() {
        val processor = processor(speed = 2f)
        processor.queueInput(pcm16(4_096, channels = 2))
        processor.reset()
        assertFalse(processor.isActive())
        assertFalse(processor.getOutput().hasRemaining())
    }
}
