package com.aritxonly.bdfun.infra.playback

import com.aritxonly.bdfun.core.application.reducePlaybackSnapshot
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.core.application.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CopyOnWriteArrayList

/** 保留原解码、音频焦点和缓冲参数；所有传输命令只落到这一适配器。 */
class Media3PlaybackController(context: Context, decoderMode: Int) : PlaybackController {
    private val appContext = context.applicationContext
    val player: ExoPlayer = buildPlayer(appContext, decoderMode)
    private val mutableState = MutableStateFlow(PlaybackSnapshot())
    override val state = mutableState.asStateFlow()
    private val listeners = CopyOnWriteArrayList<PlaybackController.Listener>()
    private var released = false
    private var mediaKey: String? = null
    private val observer = object : Player.Listener {
        override fun onRenderedFirstFrame() { listeners.forEach { it.onRenderedFirstFrame() } }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            refresh()
            if (isPlaying) mutableState.value = mutableState.value.copy(ended = false)
            listeners.forEach { it.onPlayingChanged(isPlaying) }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            refresh()
            listeners.forEach { it.onPhaseChanged(mutableState.value.phase) }
        }
        override fun onPlayerError(error: PlaybackException) {
            val failure = PlaybackFailure(error.errorCodeName, error.message)
            mutableState.value = mutableState.value.copy(error = failure)
            listeners.forEach { it.onError(failure) }
        }
    }
    init { player.addListener(observer) }

    override fun refresh(): PlaybackSnapshot {
        if (released) return mutableState.value
        val phase = when (player.playbackState) {
            Player.STATE_BUFFERING -> PlaybackPhase.Buffering
            Player.STATE_READY -> PlaybackPhase.Ready
            Player.STATE_ENDED -> PlaybackPhase.Ended
            else -> PlaybackPhase.Idle
        }
        mutableState.value = reducePlaybackSnapshot(mutableState.value,
            phase, player.isPlaying, player.playWhenReady, player.currentPosition,
            player.bufferedPosition, player.duration, player.playbackParameters.speed,
        )
        return mutableState.value
    }

    override fun prepare(source: PlaybackSource) {
        if (released) return
        val continued = continuePlaybackSource(mediaKey, source, refresh())
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000).setReadTimeoutMs(30_000).setUserAgent(source.userAgent).setDefaultRequestProperties(source.headers)
        val data = DefaultDataSource.Factory(appContext, http)
        val video: MediaSource = ProgressiveMediaSource.Factory(data).createMediaSource(MediaItem.fromUri(source.videoUrl))
        val audioUrl = source.audioUrl
        val media = if (audioUrl.isNullOrBlank()) video else MergingMediaSource(video,
            ProgressiveMediaSource.Factory(data).createMediaSource(MediaItem.fromUri(audioUrl)))
        mutableState.value = mutableState.value.copy(error = null)
        // 起始位置随媒体源一次提交，避免 prepare 后再 seek 短暂发布零进度。
        mediaKey = continued.mediaKey
        player.setMediaSource(media, continued.resumePositionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = continued.autoPlay
        player.setPlaybackSpeed(continued.speed)
        refresh()
    }
    override fun play() { if (!released) { player.play(); refresh() } }
    override fun pause() { if (!released) { player.pause(); refresh() } }
    override fun seekTo(positionMs: Long) { if (!released) { player.seekTo(positionMs); refresh() } }
    override fun setSpeed(speed: Float) { if (!released) { player.setPlaybackSpeed(speed); refresh() } }
    override fun acknowledgeEnd() { mutableState.value = mutableState.value.copy(ended = false) }
    override fun addListener(listener: PlaybackController.Listener) { listeners.addIfAbsent(listener) }
    override fun removeListener(listener: PlaybackController.Listener) { listeners.remove(listener) }
    override fun release() {
        // 后台退出入口可能从清理执行器调用，Media3 仍须在 applicationLooper 释放。
        if (android.os.Looper.myLooper() != player.applicationLooper) {
            android.os.Handler(player.applicationLooper).post { release() }
            return
        }
        if (released) return
        released = true
        player.removeListener(observer)
        listeners.clear()
        player.release()
        mutableState.value = mutableState.value.copy(playing = false, playWhenReady = false, phase = PlaybackPhase.Idle)
    }
}
