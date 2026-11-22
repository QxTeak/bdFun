@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback.tempo

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs

/**
 * 把 [WsolaTempoStretcher] 接成 Media3 的 AudioProcessor。
 *
 * 它占据内置 Sonic 在音频链里的位置：输入 PCM16 / float，输出与输入同格式，
 * 只改时长不改音高。速度 1.0 时 [isActive] 为 false，Media3 会整条旁路，
 * 保证正常播放仍然逐字节直通。
 *
 * 注意：这个类只负责“处理”，不负责接入。替换 Sonic 需要通过
 * [TempoAudioProcessorChain] 传给 `DefaultAudioSink.Builder.setAudioProcessorChain()`。
 */
internal class TempoAudioProcessor : AudioProcessor {

    var speed: Float = 1f
        set(value) {
            field = value.coerceIn(MIN_SPEED, MAX_SPEED)
            stretcher?.speed = field
        }

    private var inputFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputFormat: AudioFormat = AudioFormat.NOT_SET
    private var stretcher: WsolaTempoStretcher? = null
    private var channels = 0
    private var bytesPerSample = 0

    private val inputSamples = FloatAccumulator()
    private val outputSamples = FloatAccumulator()
    private var encodeBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var pendingOutput: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding
        if (encoding != C.ENCODING_PCM_16BIT && encoding != C.ENCODING_PCM_FLOAT) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        inputFormat = inputAudioFormat
        outputFormat = inputAudioFormat
        channels = inputAudioFormat.channelCount
        bytesPerSample = if (encoding == C.ENCODING_PCM_FLOAT) 4 else 2
        stretcher = WsolaTempoStretcher(inputAudioFormat.sampleRate, channels).also { it.speed = speed }
        return outputFormat
    }

    override fun isActive(): Boolean = stretcher != null && abs(speed - 1f) > SPEED_EPSILON

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining <= 0) return
        inputSamples.clear()
        decode(inputBuffer, remaining)
        val engine = stretcher ?: return
        outputSamples.clear()
        engine.process(inputSamples.data, 0, inputSamples.size / channels, false, outputSamples)
        inputSamples.clear()
        publishPending()
    }

    override fun queueEndOfStream() {
        val engine = stretcher
        if (engine != null) {
            outputSamples.clear()
            engine.process(EMPTY_FLOATS, 0, 0, true, outputSamples)
            inputSamples.clear()
            publishPending()
        }
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val buffer = pendingOutput
        pendingOutput = AudioProcessor.EMPTY_BUFFER
        return buffer
    }

    override fun isEnded(): Boolean = inputEnded && !pendingOutput.hasRemaining()

    override fun flush() {
        stretcher?.flush()
        inputSamples.clear()
        outputSamples.clear()
        pendingOutput = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
    }

    override fun reset() {
        flush()
        stretcher = null
        inputFormat = AudioFormat.NOT_SET
        outputFormat = AudioFormat.NOT_SET
        channels = 0
    }

    override fun getDurationAfterProcessorApplied(durationUs: Long): Long =
        if (isActive()) (durationUs.toDouble() / speed).toLong() else durationUs

    private fun decode(buffer: ByteBuffer, remaining: Int) {
        val frameCount = remaining / (bytesPerSample * channels)
        if (frameCount <= 0) {
            buffer.position(buffer.position() + remaining)
            return
        }
        // 不建 FloatBuffer/ShortBuffer 视图：android.jar 与 JDK 的协变返回签名不一致，
        // 直接用绝对下标读，语义更直白，也避开的一次性视图分配。
        val start = buffer.position()
        val total = frameCount * channels
        inputSamples.begin(total)
        val data = inputSamples.data
        if (inputFormat.encoding == C.ENCODING_PCM_FLOAT) {
            for (index in 0 until total) data[index] = buffer.getFloat(start + index * 4)
        } else {
            for (index in 0 until total) data[index] = buffer.getShort(start + index * 2) / SHORT_SCALE
        }
        buffer.position(start + total * bytesPerSample)
    }

    private fun publishPending() {
        val frames = outputSamples.size / channels
        if (frames <= 0) {
            pendingOutput = AudioProcessor.EMPTY_BUFFER
            return
        }
        val bytes = frames * channels * bytesPerSample
        var target: ByteBuffer = encodeBuffer
        if (target.capacity() < bytes) {
            target = ByteBuffer.allocateDirect(bytes)
            encodeBuffer = target
        }
        target.clear()
        target.order(ByteOrder.LITTLE_ENDIAN)
        val count = frames * channels
        if (bytesPerSample == 4) {
            for (index in 0 until count) target.putFloat(outputSamples.data[index])
        } else {
            for (index in 0 until count) {
                val value = (outputSamples.data[index] * SHORT_SCALE).toInt()
                target.putShort(
                    value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort(),
                )
            }
        }
        target.position(0)
        target.limit(bytes)
        pendingOutput = target
    }

    private companion object {
        const val SHORT_SCALE = 32768f
        const val SPEED_EPSILON = 1e-4f
        const val MIN_SPEED = 0.1f
        const val MAX_SPEED = 8f
        val EMPTY_FLOATS = FloatArray(0)
    }
}
