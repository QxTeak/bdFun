@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback.tempo

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * 离线 A/B：同一段音频分别用 Media3 Sonic 和自研 WSOLA 变速，落成 wav 供人耳对比。
 *
 * 默认不跑（避免拖慢 CI / 往仓库里写文件），只在设置了环境变量时执行：
 * ```
 * TEMPO_AB_OUT=<输出目录> [TEMPO_AB_SOURCE=<16bit wav>] ./gradlew :infra:playback:testDebugUnitTest --tests "*TempoOfflineAbTest*"
 * ```
 * 不提供 source 时用内置的类语音/类音乐合成信号，足够听出 grain 接缝和金属感。
 */
class TempoOfflineAbTest {

    @Test
    fun renderAbSamples() {
        val outDir = System.getenv("TEMPO_AB_OUT")?.takeIf { it.isNotBlank() }?.let(::File)
        assumeTrue("TEMPO_AB_OUT not set, skip offline A/B", outDir != null)
        outDir!!.mkdirs()

        val source = System.getenv("TEMPO_AB_SOURCE")?.takeIf { it.isNotBlank() }?.let(::File)
        val audio = if (source != null && source.isFile) {
            WavIo.read(source)
        } else {
            synthMaterial(44_100, 2, 9.0)
        }
        WavIo.write(File(outDir, "00_source.wav"), audio.sampleRate, audio.channels, audio.pcm)
        println("source: rate=${audio.sampleRate} ch=${audio.channels} frames=${audio.frames}")

        for (speed in listOf(1.5f, 2f, 3f)) {
            val tag = speed.toString().replace('.', '_')
            val sonic = renderSonic(audio.sampleRate, audio.channels, audio.pcm, speed)
            WavIo.write(File(outDir, "sonic_${tag}x.wav"), audio.sampleRate, audio.channels, sonic)
            val wsola = renderWsola(audio.sampleRate, audio.channels, audio.pcm, speed)
            WavIo.write(File(outDir, "wsola_${tag}x.wav"), audio.sampleRate, audio.channels, wsola)
            println(
                "speed=${speed}x sonicFrames=${sonic.size / audio.channels} " +
                    "wsolaFrames=${wsola.size / audio.channels} " +
                    "inputFrames=${audio.frames}",
            )
        }
    }

    private fun renderWsola(
        sampleRate: Int,
        channels: Int,
        pcm: ShortArray,
        speed: Float,
        chunkFrames: Int = 4096,
    ): ShortArray {
        val stretcher = WsolaTempoStretcher(sampleRate, channels).apply { this.speed = speed }
        val floats = WavIo.pcmToFloats(pcm)
        val out = FloatAccumulator()
        val totalFrames = pcm.size / channels
        var offset = 0
        while (offset < totalFrames) {
            val frames = min(chunkFrames, totalFrames - offset)
            stretcher.process(floats, offset, frames, false, out)
            offset += frames
        }
        stretcher.process(FloatArray(0), 0, 0, true, out)
        return WavIo.floatsToPcm(out.data.copyOf(out.size))
    }

    private fun renderSonic(
        sampleRate: Int,
        channels: Int,
        pcm: ShortArray,
        speed: Float,
    ): ShortArray {
        val processor = SonicAudioProcessor()
        processor.setSpeed(speed)
        processor.setPitch(1f)
        processor.configure(AudioProcessor.AudioFormat(sampleRate, channels, C.ENCODING_PCM_16BIT))
        processor.flush()
        val out = ByteArrayOutputStream()
        val chunkFrames = 4096
        var offset = 0
        val totalFrames = pcm.size / channels
        while (offset < totalFrames) {
            val frames = min(chunkFrames, totalFrames - offset)
            val buffer = ByteBuffer.allocateDirect(frames * channels * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (index in 0 until frames * channels) {
                buffer.putShort(pcm[offset * channels + index])
            }
            buffer.flip()
            processor.queueInput(buffer)
            drain(processor, out)
            offset += frames
        }
        processor.queueEndOfStream()
        var guard = 0
        while (!processor.isEnded() && guard++ < 10_000) {
            if (!drain(processor, out)) break
        }
        val bytes = out.toByteArray()
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val result = ShortArray(bytes.size / 2) { index -> buffer.getShort(index * 2) }
        return result
    }

    private fun drain(processor: AudioProcessor, target: ByteArrayOutputStream): Boolean {
        var wrote = false
        while (true) {
            val output = processor.getOutput()
            if (!output.hasRemaining()) break
            val chunk = ByteArray(output.remaining())
            output.get(chunk)
            target.write(chunk)
            wrote = true
        }
        return wrote
    }

    /**
     * 合成一段「类语音 + 类音乐」素材：
     * 前半段是带共振峰的浊音音节（最容易被 WSOLA 颤音暴露），
     * 后半段是带瞬态的分解和弦（最容易被交叉淡化糊掉）。
     */
    private fun synthMaterial(sampleRate: Int, channels: Int, seconds: Double): WavIo.Audio {
        val frames = (sampleRate * seconds).toInt()
        val mono = FloatArray(frames)
        val speechFrames = (frames * 0.55).toInt()
        var phase = 0.0
        val formants = doubleArrayOf(700.0, 1_220.0, 2_600.0)
        val resonators = Array(formants.size) { Resonator(sampleRate, formants[it], 0.93) }
        for (frame in 0 until speechFrames) {
            val t = frame.toDouble() / sampleRate
            val syllable = (t * 3.2) % 1.0
            val envelope = if (syllable < 0.72) sin(PI * syllable / 0.72).pow(2.0) else 0.0
            val f0 = 118.0 + 22.0 * sin(2.0 * PI * 0.8 * t)
            phase += f0 / sampleRate
            if (phase >= 1.0) phase -= 1.0
            val glottal = if (phase < 0.03) (1.0 - phase / 0.03) else 0.0
            var value = glottal
            for (resonator in resonators) value = resonator.process(value)
            mono[frame] = (value * envelope * 0.55).toFloat()
        }
        // 音乐段：每 0.9s 换一个三和弦，带衰减包络 + 每 0.45s 一个瞬态
        val chords = arrayOf(
            doubleArrayOf(261.63, 329.63, 392.00),
            doubleArrayOf(220.00, 261.63, 329.63),
            doubleArrayOf(174.61, 220.00, 261.63),
            doubleArrayOf(196.00, 246.94, 293.66),
        )
        for (frame in speechFrames until frames) {
            val t = (frame - speechFrames).toDouble() / sampleRate
            val chordIndex = (t / 0.9).toInt() % chords.size
            val chordT = t % 0.9
            var value = 0.0
            for (note in chords[chordIndex]) {
                for (harmonic in 1..5) {
                    val envelope = exp(-2.5 * chordT) / harmonic
                    value += envelope * sin(2.0 * PI * note * harmonic * t)
                }
            }
            val beat = t % 0.45
            if (beat < 0.01) value += sin(2.0 * PI * 90.0 * beat) * (1.0 - beat / 0.01)
            mono[frame] = (mono[frame] + value * 0.16).toFloat()
        }
        // 归一化到 -1.5dB 左右，接近 B 站音频的常见峰值
        var peak = 0f
        for (value in mono) peak = maxOf(peak, kotlin.math.abs(value))
        val gain = if (peak > 1e-6f) 0.84f / peak else 0f
        val pcm = ShortArray(frames * channels)
        for (frame in 0 until frames) {
            for (channel in 0 until channels) {
                val shifted = if (channel == 0) mono[frame] else mono[frame] * 0.85f
                val value = (shifted * gain * 32_767f).toInt()
                pcm[frame * channels + channel] =
                    value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return WavIo.Audio(sampleRate, channels, pcm)
    }

    /** 两极点共振器，用来给脉冲串加共振峰。 */
    private class Resonator(sampleRate: Int, frequency: Double, radius: Double) {
        private val a1 = 2.0 * radius * kotlin.math.cos(2.0 * PI * frequency / sampleRate)
        private val a2 = -radius * radius
        private var y1 = 0.0
        private var y2 = 0.0

        fun process(input: Double): Double {
            val output = input - a1 * y1 - a2 * y2
            y2 = y1
            y1 = output
            return output
        }
    }
}
