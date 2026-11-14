package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.aritxonly.bdfun.core.domain.LivePlayback

/**
 * 直播播放器。
 *
 * 拉流请求必须自带和网页一致的 Referer / UA / Cookie：B 站直播 CDN 会按 Referer 校验，
 * ExoPlayer 默认的 `ExoPlayerLib/…` UA 也会被部分节点拒绝，表现就是有地址但一直黑屏。
 */
class Media3LivePlayback(
    context: Context,
    private val layoutId: Int,
    cookie: String = "",
) : LivePlayback {
    private val httpDataSource = DefaultHttpDataSource.Factory()
        .setUserAgent(LIVE_UA)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(15_000)
        .setDefaultRequestProperties(
            buildMap {
                put("Referer", "https://live.bilibili.com/")
                put("Origin", "https://live.bilibili.com")
                if (cookie.isNotBlank()) put("Cookie", cookie)
            },
        )

    private val player = ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(
            DefaultMediaSourceFactory(
                DefaultDataSource.Factory(context.applicationContext, httpDataSource),
            ),
        )
        .build()
        .apply { playWhenReady = true }

    override fun prepare(url: String) {
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.play()
    }

    override fun play() = player.play()
    override fun pause() = player.pause()
    override fun release() = player.release()

    fun createView(context: Context): View = (LayoutInflater.from(context).inflate(layoutId, null) as PlayerView).apply {
        useController = false
        setShutterBackgroundColor(android.graphics.Color.BLACK)
        this.player = this@Media3LivePlayback.player
    }

    fun bindView(view: View) { (view as PlayerView).player = player }
    fun releaseView(view: View) { (view as PlayerView).player = null }

    private companion object {
        const val LIVE_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
    }
}
