package com.aritxonly.bdfun.infra.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import com.aritxonly.bdfun.core.domain.buffering.BufferSession
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

/**
 * 按硬解模式构建 ExoPlayer：
 * 0 自动 / 1 硬件解码（失败回退软解）/ 2 软件解码 / 3 硬件解码（禁止回退）。
 *
 * 倍速走自研 WSOLA（[TeakAudioRenderersFactory] → [TempoAudioProcessorChain]），
 * 不再用 ExoPlayer 默认链里的 Sonic：Sonic 只吃 PCM16、2x 以上金属感明显，
 * 而且每次改速都会 flush 重建。
 *
 * 不要打开 AudioTrack playback params：部分机型在改倍速后音轨时间戳恢复得比画面慢，
 * 长按加速松手会短暂音画不同步。
 *
 * 响度均衡仍是音频处理器链里的软件 AGC（[LoudnessNormalizationAudioProcessor]），
 * 挂在变速之前；不再用平台 LoudnessEnhancer。
 */
internal fun buildPlayer(
    context: Context,
    decoderMode: Int,
    buffering: BufferSession,
    loudnessEq: Boolean,
    diagnosticLog: ((String) -> Unit)?,
): ExoPlayer {
    val renderers = TeakAudioRenderersFactory(context, loudnessEq)
        .setEnableAudioTrackPlaybackParams(false)
        .configureVideoDecoder(decoderMode)
    val loadControl = StrategyLoadControl(buffering, diagnosticLog)
    return ExoPlayer.Builder(context, renderers)
        .setLoadControl(loadControl)
        // 前台服务不保证锁屏后 CPU/Wi-Fi 持续工作；由播放器随播放状态管理锁。
        .setWakeMode(androidx.media3.common.C.WAKE_MODE_NETWORK)
        // 让 ExoPlayer 自己申请音频焦点：相关推荐新开播放器时，旧播放器会因失焦自动暂停，
        // 转场慢的机型也不会两路声音同时出。
        .setAudioAttributes(
            androidx.media3.common.AudioAttributes.Builder()
                .setUsage(androidx.media3.common.C.USAGE_MEDIA)
                .setContentType(androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            true,
        )
        .build()
}

internal fun videoRendererFactory(context: Context, decoderMode: Int) =
    DefaultRenderersFactory(context).configureVideoDecoder(decoderMode)

private fun DefaultRenderersFactory.configureVideoDecoder(mode: Int): DefaultRenderersFactory {
    setEnableDecoderFallback(mode != 3)
    if (mode in 1..3) setMediaCodecSelector(TeakCodecSelector(mode))
    return this
}

private class TeakCodecSelector(private val mode: Int) : MediaCodecSelector {
    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean,
    ): List<MediaCodecInfo> {
        val all = MediaCodecUtil.getDecoderInfos(
            mimeType,
            requiresSecureDecoder,
            requiresTunnelingDecoder,
        )
        return when (mode) {
            // 硬件解码：硬解排在前面，硬解初始化失败时由 ExoPlayer 回退到列表里的软解。
            1 -> all.filter { !it.softwareOnly } + all.filter { it.softwareOnly }
            2 -> all.filter { it.softwareOnly }
            // 硬件解码（禁止回退）：列表里只留硬解，buildPlayer 同时关掉 fallback。
            3 -> all.filter { !it.softwareOnly }
            else -> all
        }
    }
}
