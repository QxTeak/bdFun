@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aritxonly.bdfun.infra.playback

import android.content.Context
import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.core.application.PlaybackSource
import com.aritxonly.bdfun.core.application.PlaybackVideoOutputMode
import com.aritxonly.bdfun.core.application.PlaybackVideoTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 两个输出各自拥有解码表面，首帧接力时不重绑正在显示的解码器。 */
interface Media3VideoHandoffOutput {
    fun show(player: Player, mode: PlaybackVideoOutputMode)
    fun stage(player: Player, mode: PlaybackVideoOutputMode)
    fun promote()
    fun clearStaged()
}

internal class Media3VideoHandoff(
    private val context: Context,
    private val primary: ExoPlayer,
    private val scope: CoroutineScope,
    private val decoderMode: Int,
    private val log: ((String) -> Unit)?,
) {
    private val outputTarget = MutableStateFlow<PlaybackVideoTarget?>(null)
    val target = outputTarget.asStateFlow()
    private var primaryTarget: PlaybackVideoTarget? = null
    private var output: Media3VideoHandoffOutput? = null
    private var pending: Candidate? = null
    private var active: Candidate? = null
    private var job: Job? = null
    private var videoEnabled = true
    val videoSize get() = active?.player?.videoSize ?: primary.videoSize
    val primaryVideoEnabled get() = videoEnabled && active == null

    fun attach(next: Media3VideoHandoffOutput) {
        output = next
        val current = active
        next.show(current?.player ?: primary, target.value?.mode ?: PlaybackVideoOutputMode.Standard)
        pending?.let { next.stage(it.player, it.target.mode) }
        if (current != null) {
            current.player.seekTo(primary.currentPosition)
            updatePlaying()
        }
    }

    fun detach(previous: Media3VideoHandoffOutput) {
        if (output !== previous) return
        output = null
        active?.player?.pause()
    }

    fun showPrimary(target: PlaybackVideoTarget?) {
        primaryTarget = target
        outputTarget.value = target
        output?.show(primary, target?.mode ?: PlaybackVideoOutputMode.Standard)
        stop()
        setTrackEnabled(primary, videoEnabled)
    }

    fun stage(source: PlaybackSource, data: DefaultDataSource.Factory) {
        val target = source.videoTarget ?: return
        if (active?.target == target || pending?.target == target) return
        clearPending()
        if (active != null || output == null || !videoEnabled) return
        val rendererFactory = videoRendererFactory(context, decoderMode)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(4_000, 10_000, 750, 1_500)
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
        val player = ExoPlayer.Builder(context, rendererFactory).setLoadControl(loadControl).build()
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            .build()
        val candidate = Candidate(player, target)
        pending = candidate
        player.addListener(candidate.listener)
        output?.stage(player, target.mode)
        val media = ProgressiveMediaSource.Factory(data).createMediaSource(MediaItem.fromUri(source.videoUrl))
        val position = stagingPosition(primary.currentPosition, primary.duration, primary.playWhenReady, primary.playbackParameters.speed)
        player.setMediaSource(media, position)
        player.prepare()
        log?.invoke("videoHandoff stage quality=${target.qualityId} at=$position")
        val started = SystemClock.elapsedRealtime()
        job = scope.launch {
            while (pending === candidate && SystemClock.elapsedRealtime() - started < 30_000L) {
                if (tryPromote(candidate)) break
                val offset = abs(preparedOffset(candidate))
                delay(if (offset < 180L) 8L else 24L)
            }
            if (pending === candidate) fail(candidate, "timeout")
            while (active === candidate) {
                synchronize(candidate.player)
                delay(180L)
            }
        }
    }

    private fun tryPromote(candidate: Candidate): Boolean {
        val host = output ?: return false
        if (!videoEnabled || primary.playbackState != Player.STATE_READY) return false
        if (primary.playWhenReady && !primary.isPlaying) return false
        val prepared = candidate.player
        val offsetMs = preparedOffset(candidate)
        val toleranceMs = handoffTolerance(primary.playWhenReady, primary.playbackParameters.speed)
        if (abs(offsetMs) > toleranceMs) {
            if (!primary.playWhenReady || offsetMs < -toleranceMs) {
                val nextPosition = stagingPosition(
                    primary.currentPosition, primary.duration, primary.playWhenReady, primary.playbackParameters.speed,
                )
                // 网络慢时按已缓冲的进度追到下一个同步点，旧画面在等待期间正常播放。
                if (prepared.playbackState == Player.STATE_READY) {
                    seekCandidate(candidate, nextPosition)
                }
            }
            return false
        }
        val firstFrameAt = candidate.firstFrameAtMs ?: return false
        if (prepared.playbackState != Player.STATE_READY || SystemClock.elapsedRealtime() - firstFrameAt < 32L) return false
        val remainingMs = (prepared.duration - prepared.currentPosition).coerceAtLeast(0L)
        if (prepared.totalBufferedDuration < minOf(1_200L, remainingMs)) return false
        pending = null
        active = candidate
        host.promote()
        outputTarget.value = candidate.target
        synchronize(prepared)
        setTrackEnabled(primary, false)
        log?.invoke("videoHandoff commit quality=${candidate.target.qualityId} driftMs=$offsetMs")
        return true
    }

    private fun preparedOffset(candidate: Candidate) = candidate.player.currentPosition - primary.currentPosition

    fun seek(positionMs: Long) {
        active?.player?.seekTo(positionMs)
        pending?.let {
            seekCandidate(it, stagingPosition(positionMs, primary.duration, primary.playWhenReady, primary.playbackParameters.speed))
        }
    }

    fun updatePlaying() {
        active?.player?.let(::synchronize)
    }

    fun setEnabled(enabled: Boolean) {
        videoEnabled = enabled
        setTrackEnabled(primary, enabled && active == null)
        active?.player?.let {
            setTrackEnabled(it, enabled)
            if (enabled) it.seekTo(primary.currentPosition)
        }
        updatePlaying()
    }

    private fun synchronize(video: ExoPlayer) {
        if (output == null || !videoEnabled || !primary.isPlaying) {
            if (video.playWhenReady) video.pause()
            return
        }
        val speed = primary.playbackParameters.speed
        val driftMs = video.currentPosition - primary.currentPosition
        if (abs(driftMs) > 280L) video.seekTo(primary.currentPosition)
        val followSpeed = followingVideoSpeed(speed, driftMs)
        if (video.playbackParameters.speed != followSpeed) video.setPlaybackSpeed(followSpeed)
        if (!video.playWhenReady) video.play()
    }

    private fun seekCandidate(candidate: Candidate, positionMs: Long) {
        if (candidate.player.currentPosition == positionMs) return
        candidate.firstFrameAtMs = null
        candidate.player.seekTo(positionMs)
    }

    fun stop() {
        clearPending()
        val old = active
        active = null
        old?.player?.release()
    }

    private fun clearPending() {
        job?.cancel()
        job = null
        output?.clearStaged()
        val old = pending
        pending = null
        old?.player?.release()
    }

    private fun fail(candidate: Candidate, reason: String) {
        if (candidate !== pending && candidate !== active) return
        log?.invoke("videoHandoff fallback quality=${candidate.target.qualityId} reason=$reason")
        if (candidate === pending) clearPending() else showPrimary(primaryTarget)
    }

    private inner class Candidate(val player: ExoPlayer, val target: PlaybackVideoTarget) {
        var firstFrameAtMs: Long? = null
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameAtMs = SystemClock.elapsedRealtime()
            }

            override fun onPlayerError(error: PlaybackException) {
                fail(this@Candidate, error.errorCodeName)
            }
        }
    }
}

private fun setTrackEnabled(player: ExoPlayer, enabled: Boolean) {
    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
        .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !enabled)
        .build()
}

internal fun stagingPosition(positionMs: Long, durationMs: Long, advancing: Boolean, speed: Float): Long {
    val still = positionMs.coerceAtLeast(0L)
    if (!advancing) return still
    val ahead = still + (2_000L * speed).toLong()
    return if (durationMs > still) ahead.coerceAtMost((durationMs - 250L).coerceAtLeast(still)) else ahead
}

internal fun followingVideoSpeed(speed: Float, driftMs: Long): Float = when {
    abs(driftMs) > 280L -> speed
    driftMs > 30L -> speed * 0.96f
    driftMs < -30L -> speed * 1.04f
    else -> speed
}

/** 倍速下一个屏幕帧覆盖更多媒体时间，允许相应的同步误差。 */
internal fun handoffTolerance(playing: Boolean, speed: Float): Long =
    if (playing) (32 * speed).toLong().coerceIn(22L, 80L) else 22L
