package com.aritxonly.bdfun.infra.playback.tempo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class WsolaTempoStretcherTest {

    private val sampleRate = 44_100

    private fun sine(frames: Int, frequency: Double, channels: Int, amplitude: Float = 0.4f): FloatArray =
        FloatArray(frames * channels) { index ->
            val frame = index / channels
            (amplitude * sin(2.0 * PI * frequency * frame / sampleRate)).toFloat()
        }

    private fun render(
        input: FloatArray,
        speed: Float,
        channels: Int = 1,
        chunkFrames: Int = 1024,
    ): FloatArray {
        val stretcher = WsolaTempoStretcher(sampleRate, channels).apply { this.speed = speed }
        val out = FloatAccumulator()
        val totalFrames = input.size / channels
        var offset = 0
        while (offset < totalFrames) {
            val frames = min(chunkFrames, totalFrames - offset)
            stretcher.process(input, offset, frames, false, out)
            offset += frames
        }
        stretcher.process(FloatArray(0), 0, 0, true, out)
        return out.data.copyOf(out.size)
    }

    /** Goertzel：某个频率上的能量，用来判断变速后音高有没有跑。 */
    private fun toneEnergy(
        samples: FloatArray,
        frequency: Double,
        channels: Int,
        channel: Int = 0,
    ): Double {
        val frames = samples.size / channels
        val omega = 2.0 * PI * frequency / sampleRate
        val coeff = 2.0 * cos(omega)
        var s1 = 0.0
        var s2 = 0.0
        for (index in 0 until frames) {
            val s0 = samples[index * channels + channel] + coeff * s1 - s2
            s2 = s1
            s1 = s0
        }
        return s1 * s1 + s2 * s2 - coeff * s1 * s2
    }

    @Test
    fun fasterSpeedShortensOutput() {
        val input = sine(sampleRate, 440.0, channels = 1)
        val output = render(input, speed = 2f)
        assertEquals(sampleRate / 2.0, output.size.toDouble(), sampleRate * 0.03)
    }

    @Test
    fun slowerSpeedLengthensOutput() {
        val input = sine(sampleRate, 440.0, channels = 1)
        val output = render(input, speed = 0.5f)
        assertEquals(sampleRate * 2.0, output.size.toDouble(), sampleRate * 0.06)
    }

    @Test
    fun outputIsFiniteAndDoesNotAmplify() {
        val input = sine(sampleRate / 2, 300.0, channels = 2, amplitude = 0.8f)
        val output = render(input, speed = 3f, channels = 2)
        assertTrue(output.isNotEmpty())
        output.forEach { value ->
            assertTrue("not finite", value.isFinite())
            assertTrue("amplified: $value", abs(value) <= 0.85f)
        }
    }

    @Test
    fun outputHasNoSampleToSampleClicks() {
        val input = sine(sampleRate, 440.0, channels = 1)
        var maxInputStep = 0f
        for (index in 1 until input.size) maxInputStep = maxOf(maxInputStep, abs(input[index] - input[index - 1]))
        val output = render(input, speed = 2f)
        var maxOutputStep = 0f
        for (index in 1 until output.size) maxOutputStep = maxOf(maxOutputStep, abs(output[index] - output[index - 1]))
        // WSOLA 的输出斜率不应超过原信号太多；明显超出说明 grain 接缝没对齐。
        assertTrue("maxStep=$maxOutputStep inputStep=$maxInputStep", maxOutputStep <= maxInputStep * 2.5f)
    }

    @Test
    fun pitchIsPreservedAtTwoTimes() {
        val input = sine(sampleRate, 440.0, channels = 1)
        val output = render(input, speed = 2f)
        val target = toneEnergy(output, 440.0, 1)
        val lower = toneEnergy(output, 220.0, 1)
        val upper = toneEnergy(output, 660.0, 1)
        assertTrue("target=$target lower=$lower upper=$upper", target > lower * 20)
        assertTrue("target=$target lower=$lower upper=$upper", target > upper * 20)
    }

    @Test
    fun stereoChannelsStaySeparated() {
        val frames = sampleRate
        val left = sine(frames, 440.0, channels = 1)
        val right = sine(frames, 1_760.0, channels = 1)
        val input = FloatArray(frames * 2) { index ->
            if (index % 2 == 0) left[index / 2] else right[index / 2]
        }
        val output = render(input, speed = 2f, channels = 2)
        val left440 = toneEnergy(output, 440.0, 2, channel = 0)
        val left1760 = toneEnergy(output, 1_760.0, 2, channel = 0)
        val right1760 = toneEnergy(output, 1_760.0, 2, channel = 1)
        val right440 = toneEnergy(output, 440.0, 2, channel = 1)
        assertTrue(left440 > left1760 * 10)
        assertTrue(right1760 > right440 * 10)
    }

    @Test
    fun chunkSizeDoesNotChangeOutput() {
        val input = sine(20_000, 523.25, channels = 2)
        val single = render(input, speed = 1.5f, channels = 2, chunkFrames = 20_000)
        val splitted = render(input, speed = 1.5f, channels = 2, chunkFrames = 333)
        assertEquals(single.size, splitted.size)
        var maxDiff = 0f
        for (index in single.indices) maxDiff = maxOf(maxDiff, abs(single[index] - splitted[index]))
        assertEquals(0f, maxDiff, 1e-5f)
    }

    @Test
    fun unitySpeedRoundTripsClosely() {
        val input = sine(8_000, 300.0, channels = 1)
        val output = render(input, speed = 1f)
        assertEquals(input.size.toDouble(), output.size.toDouble(), 64.0)
        val compared = min(input.size, output.size)
        var maxDiff = 0f
        for (index in 0 until compared) maxDiff = maxOf(maxDiff, abs(input[index] - output[index]))
        assertEquals(0f, maxDiff, 1e-4f)
    }

    @Test
    fun silenceStaysSilent() {
        val input = FloatArray(sampleRate)
        val output = render(input, speed = 2f)
        output.forEach { assertEquals(0f, it, 1e-6f) }
    }

    @Test
    fun flushDropsBufferedState() {
        val stretcher = WsolaTempoStretcher(sampleRate, 1).apply { speed = 2f }
        val out = FloatAccumulator()
        stretcher.process(sine(5_000, 440.0, 1), 0, 5_000, false, out)
        assertTrue(out.size > 0)
        stretcher.flush()
        out.clear()
        stretcher.process(sine(5_000, 440.0, 1), 0, 5_000, false, out)
        // flush 之后重新计数，输出长度只与新一轮输入有关。
        assertEquals(2_500.0, out.size.toDouble(), 5_000 * 0.05)
    }

    @Test
    fun speedChangeKeepsStateAndDoesNotReset() {
        val stretcher = WsolaTempoStretcher(sampleRate, 1)
        val out = FloatAccumulator()
        val input = sine(sampleRate, 440.0, 1)
        stretcher.speed = 2f
        stretcher.process(input, 0, 20_000, false, out)
        stretcher.speed = 3f
        stretcher.process(input, 20_000, 20_000, false, out)
        assertTrue(out.size > 0)
        out.data.copyOf(out.size).forEach { assertTrue(it.isFinite()) }
    }
}
