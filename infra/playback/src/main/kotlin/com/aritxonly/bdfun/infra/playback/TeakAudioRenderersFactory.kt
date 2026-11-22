@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback

import android.content.Context
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import com.aritxonly.bdfun.infra.playback.tempo.TempoAudioProcessor
import com.aritxonly.bdfun.infra.playback.tempo.TempoAudioProcessorChain

/**
 * 全局音频渲染工厂：倍速走自研 WSOLA，响度均衡按开关挂在它前面。
 *
 * 这里用 [TempoAudioProcessorChain] 顶掉 `DefaultAudioSink` 默认链（默认链里是 Sonic）：
 * - Sonic 只吃 PCM16，2x 以上金属感明显，且每次改速都会被 flush 重建；
 * - 自研链全程 float、改速不重建，倍速时音色与 mpv 的 scaletempo2 同一档。
 *
 * 注意：一旦换了 chain，默认链里的 SilenceSkipping 也不再参与，静音跳过恒为关闭；
 * 播放器侧即使调用 `setSkipSilenceEnabled(true)` 也不会生效，这是刻意的取舍。
 */
internal class TeakAudioRenderersFactory(
    context: Context,
    private val loudnessEq: Boolean,
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink {
        // 处理器在每次建 sink 时新建：同一个实例不能同时接到两条音频链上。
        val tempoProcessor = TempoAudioProcessor()
        val processors: Array<AudioProcessor> = if (loudnessEq) {
            arrayOf(LoudnessNormalizationAudioProcessor(), tempoProcessor)
        } else {
            arrayOf(tempoProcessor)
        }
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
            .setAudioProcessorChain(TempoAudioProcessorChain(processors, tempoProcessor))
            .build()
    }
}
