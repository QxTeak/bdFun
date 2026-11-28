package com.aritxonly.bdfun.infra.playback

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import com.aritxonly.bdfun.core.application.reducePlaybackSnapshot
import android.content.Context
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import com.aritxonly.bdfun.core.application.*
import kotlinx.coroutines.*
import com.aritxonly.bdfun.core.domain.playbackNetworkRetryDelay
import com.aritxonly.bdfun.core.domain.playbackRestartPositionMs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/** 保留原解码、音频焦点；所有传输命令只落到这一适配器。 */
@OptIn(UnstableApi::class)
class Media3PlaybackController(
    context: Context,
    decoderMode: Int,
    private val buffering: com.aritxonly.bdfun.core.domain.buffering.BufferSession,
    private val bufferOptions: () -> Pair<String, Boolean>,
    loudnessEq: Boolean,
    diagnosticLog: ((String) -> Unit)? = null,
) : PlaybackController {
    private val appContext = context.applicationContext
    val player: ExoPlayer = buildPlayer(appContext, decoderMode, buffering, loudnessEq, diagnosticLog)
    private val diagnostics = diagnosticLog?.let { PlaybackDiagnostics(appContext, it) }
    private val mutableState = MutableStateFlow(PlaybackSnapshot())
    override val state = mutableState.asStateFlow()
    override val currentMediaUrl: String?
        get() = lastSource?.let { it.castUrl?.takeIf(String::isNotBlank) ?: it.videoUrl }
            ?.takeIf { it.isNotBlank() }
    private val listeners = CopyOnWriteArrayList<PlaybackController.Listener>()
    private var released = false
    private var lastSource: PlaybackSource? = null
    private var sourceRevision = 0L
    private var retryCount = 0
    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val videoHandoff = Media3VideoHandoff(appContext, player, recoveryScope, decoderMode, diagnosticLog)
    val videoOutputTarget = videoHandoff.target
    private var retryJob: Job? = null
    private var mediaKey: String? = null

    // ---- DASH 自适应源 ----
    // 同一内容的换档只改轨道选择；这些字段记录当前播放器里装的是哪份 MPD。
    private var dashMediaKey: String? = null
    private var dashManifestSignature: String? = null
    private var dashTracks: List<PlaybackDashTrack> = emptyList()
    private var dashQuality = 0
    private var pendingDashQuality: Int? = null
    /** DASH 首帧前失败时只回退一次渐进流，避免坏 MPD 把页面卡死。 */
    private var dashFallbackDone = false
    private var renderedFirstFrame = false
    private var dashWatchdogJob: Job? = null

    private val observer = object : Player.Listener {
        override fun onRenderedFirstFrame() {
            renderedFirstFrame = true
            dashWatchdogJob?.cancel()
            listeners.forEach { it.onRenderedFirstFrame() }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            videoHandoff.updatePlaying()
            refresh()
            if (isPlaying) mutableState.value = mutableState.value.copy(ended = false)
            listeners.forEach { it.onPlayingChanged(isPlaying) }
        }
        override fun onPlaybackStateChanged(playbackState: Int) {
            refresh()
            listeners.forEach { it.onPhaseChanged(mutableState.value.phase) }
        }
        override fun onTracksChanged(tracks: Tracks) {
            // 轨道表刚就绪时补一次挂起的换档；换档本身不会再重建 MediaSource。
            pendingDashQuality?.let { quality -> applyDashVideoQuality(quality) }
        }
        override fun onPlayerError(error: PlaybackException) {
            buffering.invalidate()
            val failure = PlaybackFailure(error.errorCodeName, error.message)
            mutableState.value = mutableState.value.copy(error = failure)
            // 本地 MPD 的兼容性因设备而异：首帧前失败先退回渐进流，别让整页直接报错。
            val source = lastSource
            if (!renderedFirstFrame && !dashFallbackDone && source?.dash != null) {
                dashFallbackDone = true
                clearDash()
                prepareSource(source.copy(dash = null), recovering = true)
                return
            }
            if (!retryNetworkFailure(failure)) listeners.forEach { it.onError(failure) }
        }
    }
    init {
        player.addListener(observer)
        diagnostics?.let(player::addAnalyticsListener)
    }

    override fun currentPositionMs(): Long = if (released) 0L else player.currentPosition

    override fun refresh(): PlaybackSnapshot {
        if (released) return mutableState.value
        diagnostics?.sample(player)
        val phase = when (player.playbackState) {
            Player.STATE_BUFFERING -> PlaybackPhase.Buffering
            Player.STATE_READY -> PlaybackPhase.Ready
            Player.STATE_ENDED -> PlaybackPhase.Ended
            else -> PlaybackPhase.Idle
        }
        val (owner, enabled) = bufferOptions()
        buffering.configure(owner, enabled)
        buffering.sample(android.os.SystemClock.elapsedRealtime(), player.isPlaying,
            phase == PlaybackPhase.Buffering, phase == PlaybackPhase.Ended)
        val videoSize = videoHandoff.videoSize
        // 值没变就不重新发布：暂停/缓冲时 refresher 仍可安全调用，但不会无意义地惊动观察者。
        val next = reducePlaybackSnapshot(mutableState.value,
            phase, player.isPlaying, player.playWhenReady, player.currentPosition,
            player.bufferedPosition, player.duration, player.playbackParameters.speed,
        ).copy(videoWidth = videoSize.width.coerceAtLeast(0), videoHeight = videoSize.height.coerceAtLeast(0))
        if (next != mutableState.value) mutableState.value = next
        return mutableState.value
    }

    override fun prepare(source: PlaybackSource) {
        if (released) return
        if (source.videoTarget?.upgradeInBackground == true && source.mediaKey != null &&
            source.mediaKey == mediaKey && lastSource != null) {
            videoHandoff.stage(source, dataSource(source))
            return
        }
        videoHandoff.showPrimary(source.videoTarget)
        val dash = source.dash
        // 同一视频/分 P 只换画质：直接改轨道选择，播放位置和音频轨都不动。
        if (dash != null && canSwitchDashInPlace(source)) {
            if (selectDashQuality(dash.videoQualityId)) {
                lastSource = source
                refresh()
                return
            }
        }
        prepareSource(source, recovering = false)
    }

    override fun selectVideoQuality(qualityId: Int): Boolean {
        if (released || dashMediaKey == null || dashMediaKey != mediaKey) return false
        if (dashTracks.none { it.id == qualityId }) return false
        if (player.currentTracks.groups.isEmpty()) {
            // 轨道还没就绪：先挂起，onTracksChanged 里补上，不重建播放源。
            pendingDashQuality = qualityId
            dashQuality = qualityId
            return true
        }
        return applyDashVideoQuality(qualityId)
    }

    private fun canSwitchDashInPlace(source: PlaybackSource): Boolean {
        val dash = source.dash ?: return false
        return dashMediaKey != null && dashMediaKey == source.mediaKey &&
            dashManifestSignature == dash.manifest.signature()
    }

    private fun prepareSource(source: PlaybackSource, recovering: Boolean) {
        if (released) return
        if (recovering) videoHandoff.showPrimary(source.videoTarget)
        if (!recovering) {
            sourceRevision++
            retryJob?.cancel()
            retryCount = 0
            dashFallbackDone = false
        }
        lastSource = source
        val continued = continuePlaybackSource(mediaKey, source, refresh())
        buffering.begin(source.mediaKey ?: source.videoUrl)
        val data = dataSource(source)
        val dash = source.dash
        val media = (dash?.let { createDashMediaSource(it, data) }) ?: createProgressiveMediaSource(source, data)
        mutableState.value = mutableState.value.copy(error = null)
        // 起始位置随媒体源一次提交，避免 prepare 后再 seek 短暂发布零进度。
        mediaKey = continued.mediaKey
        renderedFirstFrame = false
        if (dash != null && media is DashMediaSource) {
            dashMediaKey = continued.mediaKey
            dashManifestSignature = dash.manifest.signature()
            dashTracks = dash.videoTracks
            dashQuality = dash.videoQualityId
            pendingDashQuality = dash.videoQualityId
            constrainInitialVideo(dash)
            armDashWatchdog()
        } else {
            clearDash()
        }
        player.setMediaSource(media, continued.resumePositionMs.coerceAtLeast(0))
        player.prepare()
        player.playWhenReady = continued.autoPlay
        // 与 BiliPai 一致：显式给 pitch=1.0，避免某些设备/Sonic 组合把音高带偏。
        player.playbackParameters = PlaybackParameters(continued.speed.coerceAtLeast(0.1f), 1f)
        refresh()
    }

    private fun dataSource(source: PlaybackSource): DefaultDataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(15_000).setReadTimeoutMs(30_000)
            .setUserAgent(source.userAgent).setDefaultRequestProperties(source.headers)
        return DefaultDataSource.Factory(appContext, http)
    }

    fun attachVideoOutput(output: Media3VideoHandoffOutput) {
        if (!released) videoHandoff.attach(output)
    }

    fun detachVideoOutput(output: Media3VideoHandoffOutput) = videoHandoff.detach(output)

    fun setVideoEnabled(enabled: Boolean) {
        if (!released) videoHandoff.setEnabled(enabled)
    }

    private fun createProgressiveMediaSource(source: PlaybackSource, data: DefaultDataSource.Factory): MediaSource {
        val video: MediaSource = ProgressiveMediaSource.Factory(data).createMediaSource(MediaItem.fromUri(source.videoUrl))
        val audioUrl = source.audioUrl
        return if (audioUrl.isNullOrBlank()) video else MergingMediaSource(video,
            ProgressiveMediaSource.Factory(data).createMediaSource(MediaItem.fromUri(audioUrl)))
    }

    /**
     * 把 MPD 写成 cache 文件再交给 DashMediaSource：B 站没有现成 manifest 地址，
     * 只能自己拼。文件按内容 hash 复用，同一视频旋转/切档不会重复写盘。
     */
    private fun createDashMediaSource(dash: PlaybackDashSource, data: DefaultDataSource.Factory): MediaSource? {
        val file = runCatching {
            val dir = File(appContext.cacheDir, "dash_manifests").apply { mkdirs() }
            val target = File(dir, "dash_${dash.manifest.signature()}.mpd")
            if (!target.exists() || target.length() == 0L) target.writeText(dash.manifest)
            target
        }.getOrNull() ?: return null
        return DashMediaSource.Factory(data).createMediaSource(
            MediaItem.Builder()
                .setUri(android.net.Uri.fromFile(file))
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build(),
        )
    }

    /** 首帧前把可选分辨率压到目标档位：否则 ExoPlayer 可能先按最高码率起播再被 override 切下来。 */
    private fun constrainInitialVideo(dash: PlaybackDashSource) {
        val track = dash.videoTracks.firstOrNull { it.id == dash.videoQualityId } ?: return
        if (track.width <= 0 || track.height <= 0) return
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(track.width, track.height)
            .apply {
                if (track.bandwidth > 0) {
                    setMaxVideoBitrate(track.bandwidth.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                }
            }
            .build()
    }

    /**
     * MPD 能出声但视频轨一直不出帧时 ExoPlayer 不会报错：首帧前超时也退回一次渐进流，
     * 宁可回到旧的换源方式，也不要让用户对着一块黑屏。
     */
    private fun armDashWatchdog() {
        dashWatchdogJob?.cancel()
        val revision = sourceRevision
        dashWatchdogJob = recoveryScope.launch {
            delay(4_000)
            val source = lastSource ?: return@launch
            if (released || renderedFirstFrame || dashFallbackDone) return@launch
            if (revision != sourceRevision || source.dash == null) return@launch
            dashFallbackDone = true
            clearDash()
            prepareSource(source.copy(dash = null), recovering = true)
        }
    }

    /** 在当前 TrackGroup 里找到目标档位并锁定；音频组不碰。 */
    private fun applyDashVideoQuality(qualityId: Int): Boolean {
        val track = dashTracks.firstOrNull { it.id == qualityId } ?: return false
        val group = player.currentTracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO } ?: return false
        val index = (0 until group.length).indexOfFirst { i ->
            val format = group.getTrackFormat(i)
            format.id == qualityId.toString() ||
                (format.width == track.width && format.height == track.height &&
                    (track.bandwidth <= 0 || format.bitrate.toLong() == track.bandwidth))
        }
        if (index < 0) return false
        pendingDashQuality = null
        dashQuality = qualityId
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, !videoHandoff.primaryVideoEnabled)
            .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
            .build()
        return true
    }

    private fun selectDashQuality(qualityId: Int): Boolean {
        if (dashTracks.none { it.id == qualityId }) return false
        if (player.currentTracks.groups.isEmpty()) {
            pendingDashQuality = qualityId
            dashQuality = qualityId
            return true
        }
        return applyDashVideoQuality(qualityId)
    }

    private fun clearDash() {
        dashWatchdogJob?.cancel()
        dashWatchdogJob = null
        dashMediaKey = null
        dashManifestSignature = null
        dashTracks = emptyList()
        dashQuality = 0
        pendingDashQuality = null
    }

    private fun retryNetworkFailure(failure: PlaybackFailure): Boolean {
        if (released || lastSource == null) return false
        val waitMs = playbackNetworkRetryDelay(failure.code, retryCount) ?: return false
        val revision = sourceRevision
        retryCount++
        retryJob?.cancel()
        retryJob = recoveryScope.launch {
            delay(waitMs)
            if (released || revision != sourceRevision) return@launch
            val current = refresh()
            val source = lastSource ?: return@launch
            if (retryCount <= 1) {
                // 第一次重试只在同一媒体源内恢复：已经缓冲的片段和解码器都留着，
                // 网络抖一下不用整条重搭，恢复更快。
                player.seekTo(current.positionMs)
                videoHandoff.seek(current.positionMs)
                player.playWhenReady = current.playWhenReady
                player.prepare()
                refresh()
            } else {
                // 再不行才换一次新地址重搭（和原来一致）。
                prepareSource(source.copy(resumePositionMs = current.positionMs,
                    autoPlay = current.playWhenReady, speed = current.speed), recovering = true)
            }
        }
        return true
    }
    override fun play() {
        if (released) return
        // ExoPlayer 停在 Ended 时 playWhenReady 仍是 true，单发 play() 是空操作：
        // 必须先回到起点，用户点播放才真的能重看（拖进度条能重播就是这个原因）。
        playbackRestartPositionMs(player.playbackState == Player.STATE_ENDED)?.let {
            player.seekTo(it)
            // seek 之后到 Ready 之间 phase 仍是 Ended，「播放完成」会多挂一会儿，这里直接收掉。
            mutableState.value = mutableState.value.copy(ended = false)
        }
        player.play()
        refresh()
    }
    override fun pause() { if (!released) { player.pause(); refresh() } }
    override fun seekTo(positionMs: Long) {
        if (!released) {
            player.seekTo(positionMs)
            videoHandoff.seek(positionMs)
            refresh()
        }
    }
    override fun setSpeed(speed: Float) {
        if (!released) {
            player.playbackParameters = PlaybackParameters(speed.coerceAtLeast(0.1f), 1f)
            videoHandoff.updatePlaying()
            refresh()
        }
    }
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
        refresh()
        buffering.finish()
        released = true
        sourceRevision++
        videoHandoff.stop()
        recoveryScope.cancel()
        player.removeListener(observer)
        listeners.clear()
        player.release()
        clearDash()
        mutableState.value = mutableState.value.copy(playing = false, playWhenReady = false, phase = PlaybackPhase.Idle)
    }
}

/** MPD 内容签名：同一份 manifest 不重复写盘，也用来判断“同一个自适应源”。 */
private fun String.signature(): String = "${length}_${hashCode()}"
