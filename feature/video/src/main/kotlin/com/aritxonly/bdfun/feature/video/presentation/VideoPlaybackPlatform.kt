package com.aritxonly.bdfun.feature.video.presentation

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import com.aritxonly.bdfun.core.application.ForegroundMediaSession
import com.aritxonly.bdfun.core.application.PlaybackController

/** 一个播放器组合的原生接缝；展示不读取解码器、Media3 轨道或平台会话实现。 */
interface VideoPlaybackPlatform : PlaybackController {
    val userAgent: String
    fun createOutput(context: Context): VideoOutput
    fun setVideoEnabled(enabled: Boolean)
    fun createMediaSession(
        context: Context, title: String, artist: String, artworkUrl: String?,
        onQueueSelected: (Int) -> Unit,
    ): ForegroundMediaSession
}

interface VideoOutput {
    val view: View
    var resizeMode: Int
    fun captureThirdSize(): Bitmap?
}
