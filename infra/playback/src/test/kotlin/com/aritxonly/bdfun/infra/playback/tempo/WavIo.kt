package com.aritxonly.bdfun.infra.playback.tempo

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 离线 A/B 用的极简 WAV 读写（只支持 16bit PCM）。
 *
 * 单测里用来把「原音 / Sonic 变速 / 自研变速」落成可以直接在电脑上听的 wav。
 */
internal object WavIo {

    class Audio(val sampleRate: Int, val channels: Int, val pcm: ShortArray) {
        val frames: Int get() = pcm.size / channels
    }

    fun read(file: File): Audio {
        val bytes = file.readBytes()
        require(bytes.size > 44 && String(bytes, 0, 4) == "RIFF" && String(bytes, 8, 4) == "WAVE") {
            "not a wav file: $file"
        }
        var channels = 1
        var sampleRate = 44_100
        var bits = 16
        var dataOffset = -1
        var dataSize = 0
        var cursor = 12
        while (cursor + 8 <= bytes.size) {
            val id = String(bytes, cursor, 4)
            val size = intAt(bytes, cursor + 4)
            val body = cursor + 8
            when (id) {
                "fmt " -> {
                    channels = shortAt(bytes, body + 2).toInt()
                    sampleRate = intAt(bytes, body + 4)
                    bits = shortAt(bytes, body + 14).toInt()
                }

                "data" -> {
                    dataOffset = body
                    dataSize = size
                }
            }
            cursor = body + size + (size and 1)
        }
        require(bits == 16) { "only 16-bit pcm is supported, got $bits" }
        require(dataOffset >= 0) { "no data chunk in $file" }
        val available = minOf(dataSize, bytes.size - dataOffset) / 2
        val pcm = ShortArray(available)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        for (index in pcm.indices) pcm[index] = buffer.getShort(dataOffset + index * 2)
        return Audio(sampleRate, channels, pcm)
    }

    fun write(file: File, sampleRate: Int, channels: Int, pcm: ShortArray) {
        val dataBytes = pcm.size * 2
        val buffer = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray())
        buffer.putInt(36 + dataBytes)
        buffer.put("WAVE".toByteArray())
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * 2)
        buffer.putShort((channels * 2).toShort())
        buffer.putShort(16)
        buffer.put("data".toByteArray())
        buffer.putInt(dataBytes)
        pcm.forEach { buffer.putShort(it) }
        file.parentFile?.mkdirs()
        file.writeBytes(buffer.array())
    }

    fun floatsToPcm(samples: FloatArray): ShortArray = ShortArray(samples.size) { index ->
        val value = (samples[index] * 32_767f).toInt()
        value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
    }

    fun pcmToFloats(pcm: ShortArray): FloatArray = FloatArray(pcm.size) { index ->
        pcm[index] / 32_768f
    }

    private fun intAt(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or
            ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
            ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
            ((bytes[offset + 3].toInt() and 0xFF) shl 24)

    private fun shortAt(bytes: ByteArray, offset: Int): Short =
        (((bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8))).toShort()
}
