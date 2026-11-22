@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback.tempo

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessorChain

/**
 * 用 [TempoAudioProcessor] 顶掉 Media3 默认的 Sonic 变速。
 *
 * `DefaultAudioSink` 的速度、进度换算都从这里走：
 * - [applyPlaybackParameters] 只把速度下发给处理器，不触发 Sonic 重建；
 * - [getMediaDuration] 把「已播出的输出时长」换算回「媒体时长」，
 *   倍速下等价于 `playout * speed`，和 ExoPlayer 自己的估算公式一致；
 * - 静音跳过保持关闭，避免和自定义处理器叠加后进度对不上。
 *
 * 接入方式（下一步才在播放器装配里打开）：
 * ```
 * DefaultAudioSink.Builder(context)
 *     .setAudioProcessorChain(TempoAudioProcessorChain(arrayOf(tempo), tempo))
 * ```
 */
internal class TempoAudioProcessorChain(
    private val processors: Array<AudioProcessor>,
    private val tempo: TempoAudioProcessor,
) : AudioProcessorChain {

    override fun getAudioProcessors(): Array<AudioProcessor> = processors

    override fun applyPlaybackParameters(playbackParameters: PlaybackParameters): PlaybackParameters {
        tempo.speed = playbackParameters.speed
        // 音高恒定为 1：本处理器只做时长伸缩，不支持变调。
        return PlaybackParameters(playbackParameters.speed, 1f)
    }

    override fun applySkipSilenceEnabled(skipSilenceEnabled: Boolean): Boolean = false

    override fun getMediaDuration(playoutDuration: Long): Long =
        if (tempo.isActive()) (playoutDuration.toDouble() * tempo.speed).toLong() else playoutDuration

    override fun getSkippedOutputFrameCount(): Long = 0L
}
