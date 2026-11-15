package com.aritxonly.bdfun.infra.playback

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

/**
 * 按硬解模式构建 ExoPlayer：
 * 0 自动 / 1 硬件解码（失败回退软解）/ 2 软件解码 / 3 硬件解码（禁止回退）。
 */
internal fun buildPlayer(context: Context, decoderMode: Int): ExoPlayer {
    val renderers = DefaultRenderersFactory(context)
        .setEnableDecoderFallback(decoderMode != 3)
        // 不要打开 AudioTrack playback params：部分机型在改倍速后音轨时间戳
        // 恢复得比画面慢，长按加速松手会短暂音画不同步。这里用 Sonic 的常规
        // 变速路径，换一点跟手速度，换稳定的音画同步。
        .setEnableAudioTrackPlaybackParams(false)
    if (decoderMode in 1..3) {
        renderers.setMediaCodecSelector(TeakCodecSelector(decoderMode))
    }
    val loadControl = DefaultLoadControl.Builder()
        // 增大低水位并在卡顿后多攒几秒，减少弱网下反复耗尽；首次起播不加等待。
        .setBufferDurationsMs(45_000, 120_000, 1_000, 5_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
    return ExoPlayer.Builder(context, renderers)
        .setLoadControl(loadControl)
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
