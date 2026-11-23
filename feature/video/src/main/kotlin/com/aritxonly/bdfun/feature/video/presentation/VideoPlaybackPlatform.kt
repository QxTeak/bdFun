package com.aritxonly.bdfun.feature.video.presentation

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.aritxonly.bdfun.core.application.ForegroundMediaSession
import com.aritxonly.bdfun.core.application.PlaybackController

/** 一个播放器组合的原生接缝；展示不读取解码器、Media3 轨道或平台会话实现。 */
interface VideoPlaybackPlatform : PlaybackController {
    val userAgent: String

    /**
     * [direct] = true 时创建能直通显示表面的输出（杜比视界必须这样，见 isDolbyVisionQuality）。
     * 其余画质保持原纹理输出，缩放/镜像/色觉辅助/抓帧都不受影响。
     */
    fun createOutput(context: Context, direct: Boolean = false): VideoOutput
    fun setVideoEnabled(enabled: Boolean)
    fun createMediaSession(
        context: Context, title: String, artist: String, artworkUrl: String?,
        onQueueSelected: (Int) -> Unit,
    ): ForegroundMediaSession
}

interface VideoOutput {
    val view: View
    var resizeMode: Int

    /** 只有纹理输出能抓帧；直通表面返回 null，调用方按“没有背景图”处理。 */
    fun captureThirdSize(): Bitmap?

    /** 换输出表面前先解绑，避免旧视图把新表面清掉。 */
    fun release()
}
