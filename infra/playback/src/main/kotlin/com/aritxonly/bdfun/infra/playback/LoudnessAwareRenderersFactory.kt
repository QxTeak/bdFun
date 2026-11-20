@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * 在默认渲染工厂之上注入软件响度均衡。
 *
 * 与 BiliPai 一致：只有「响度均衡」开启时才选这个工厂；注入 AudioProcessor
 * 会旁路 offload，PCM 处理链保持生效。倍速仍走 Sonic，不使用 AudioTrack params。
 */
internal class LoudnessAwareRenderersFactory(
    context: Context,
) : DefaultRenderersFactory(context) {

    private val loudnessProcessor = LoudnessNormalizationAudioProcessor()

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
        .setAudioProcessors(arrayOf(loudnessProcessor as AudioProcessor))
        .build()
}
