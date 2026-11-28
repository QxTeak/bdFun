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

/** 淇濈暀鍘熻В鐮併€侀煶棰戠劍鐐癸紱鎵€鏈変紶杈撳懡浠ゅ彧钀藉埌杩欎竴閫傞厤鍣ㄣ€?*/
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

    // ---- DASH 鑷€傚簲婧?----
    // 鍚屼竴鍐呭鐨勬崲妗ｅ彧鏀硅建閬撻€夋嫨锛涜繖浜涘瓧娈佃褰曞綋鍓嶆挱鏀惧櫒閲岃鐨勬槸鍝唤 MPD銆?
    private var dashMediaKey: String? = null
    private var dashManifestSignature: String? = null
    private var dashTracks: List<PlaybackDashTrack> = emptyList()
    private var dashQuality = 0
    private var pendingDashQuality: Int? = null
    /** DASH 棣栧抚鍓嶅け璐ユ椂鍙洖閫€涓€娆℃笎杩涙祦锛岄伩鍏嶅潖 MPD 鎶婇〉闈㈠崱姝汇€?*/
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
            // 杞ㄩ亾琛ㄥ垰灏辩华鏃惰ˉ涓€娆℃寕璧风殑鎹㈡。锛涙崲妗ｆ湰韬笉浼氬啀閲嶅缓 MediaSource銆?
            pendingDashQuality?.let { quality -> applyDashVideoQuality(quality) }
        }
        override fun onPlayerError(error: PlaybackException) {
            buffering.invalidate()
            val failure = PlaybackFailure(error.errorCodeName, error.message)
            mutableState.value = mutableState.value.copy(error = failure)
            // 鏈湴 MPD 鐨勫吋瀹规€у洜璁惧鑰屽紓锛氶甯у墠澶辫触鍏堥€€鍥炴笎杩涙祦锛屽埆璁╂暣椤电洿鎺ユ姤閿欍€?
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
        // 鍊兼病鍙樺氨涓嶉噸鏂板彂甯冿細鏆傚仠/缂撳啿鏃?refresher 浠嶅彲瀹夊叏璋冪敤锛屼絾涓嶄細鏃犳剰涔夊湴鎯婂姩瑙傚療鑰呫€?
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
        // 鍚屼竴瑙嗛/鍒?P 鍙崲鐢昏川锛氱洿鎺ユ敼杞ㄩ亾閫夋嫨锛屾挱鏀句綅缃拰闊抽杞ㄩ兘涓嶅姩銆?
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
            // 杞ㄩ亾杩樻病灏辩华锛氬厛鎸傝捣锛宱nTracksChanged 閲岃ˉ涓婏紝涓嶉噸寤烘挱鏀炬簮銆?
            pendingDashQuality = qualityId
            dashQuality = qualityId
            return true
        }
        return applyDashVideoQuality(qualityId)
    }

    private fun canSwitchDashInPlace(source: PlaybackSource): Boolean {
        val dash = source.dash ?: return false
        return dashMediaKey != null && dashMediaKey == source.mediaKey &&
            dashManifestSignature == dash.manifest.playbackSignature()
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
        // 璧峰浣嶇疆闅忓獟浣撴簮涓€娆℃彁浜わ紝閬垮厤 prepare 鍚庡啀 seek 鐭殏鍙戝竷闆惰繘搴︺€?
        mediaKey = continued.mediaKey
        renderedFirstFrame = false
        if (dash != null && media is DashMediaSource) {
            dashMediaKey = continued.mediaKey
            dashManifestSignature = dash.manifest.playbackSignature()
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
        // 涓?BiliPai 涓€鑷达細鏄惧紡缁?pitch=1.0锛岄伩鍏嶆煇浜涜澶?Sonic 缁勫悎鎶婇煶楂樺甫鍋忋€?
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
     * 鎶?MPD 鍐欐垚 cache 鏂囦欢鍐嶄氦缁?DashMediaSource锛欱 绔欐病鏈夌幇鎴?manifest 鍦板潃锛?
     * 鍙兘鑷繁鎷笺€傛枃浠舵寜鍐呭 hash 澶嶇敤锛屽悓涓€瑙嗛鏃嬭浆/鍒囨。涓嶄細閲嶅鍐欑洏銆?
     */
    private fun createDashMediaSource(dash: PlaybackDashSource, data: DefaultDataSource.Factory): MediaSource? {
        val file = runCatching {
            val dir = File(appContext.cacheDir, "dash_manifests").apply { mkdirs() }
            val target = File(dir, "dash_${dash.manifest.playbackSignature()}.mpd")
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
    /** 棣栧抚鍓嶆妸鍙€夊垎杈ㄧ巼鍘嬪埌鐩爣妗ｄ綅锛氬惁鍒?ExoPlayer 鍙兘鍏堟寜鏈€楂樼爜鐜囪捣鎾啀琚?override 鍒囦笅鏉ャ€?*/
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
     * MPD 鑳藉嚭澹颁絾瑙嗛杞ㄤ竴鐩翠笉鍑哄抚鏃?ExoPlayer 涓嶄細鎶ラ敊锛氶甯у墠瓒呮椂涔熼€€鍥炰竴娆℃笎杩涙祦锛?
     * 瀹佸彲鍥炲埌鏃х殑鎹㈡簮鏂瑰紡锛屼篃涓嶈璁╃敤鎴峰鐫€涓€鍧楅粦灞忋€?
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
    /** 鍦ㄥ綋鍓?TrackGroup 閲屾壘鍒扮洰鏍囨。浣嶅苟閿佸畾锛涢煶棰戠粍涓嶇銆?*/
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
                // 绗竴娆￠噸璇曞彧鍦ㄥ悓涓€濯掍綋婧愬唴鎭㈠锛氬凡缁忕紦鍐茬殑鐗囨鍜岃В鐮佸櫒閮界暀鐫€锛?
                // 缃戠粶鎶栦竴涓嬩笉鐢ㄦ暣鏉￠噸鎼紝鎭㈠鏇村揩銆?
                player.seekTo(current.positionMs)
                videoHandoff.seek(current.positionMs)
                player.playWhenReady = current.playWhenReady
                player.prepare()
                refresh()
            } else {
                // 鍐嶄笉琛屾墠鎹竴娆℃柊鍦板潃閲嶆惌锛堝拰鍘熸潵涓€鑷达級銆?
                prepareSource(source.copy(resumePositionMs = current.positionMs,
                    autoPlay = current.playWhenReady, speed = current.speed), recovering = true)
            }
        }
        return true
    }
    override fun play() {
        if (released) return
        // ExoPlayer 鍋滃湪 Ended 鏃?playWhenReady 浠嶆槸 true锛屽崟鍙?play() 鏄┖鎿嶄綔锛?
        // 蹇呴』鍏堝洖鍒拌捣鐐癸紝鐢ㄦ埛鐐规挱鏀炬墠鐪熺殑鑳介噸鐪嬶紙鎷栬繘搴︽潯鑳介噸鎾氨鏄繖涓師鍥狅級銆?
        playbackRestartPositionMs(player.playbackState == Player.STATE_ENDED)?.let {
            player.seekTo(it)
            // seek 涔嬪悗鍒?Ready 涔嬮棿 phase 浠嶆槸 Ended锛屻€屾挱鏀惧畬鎴愩€嶄細澶氭寕涓€浼氬効锛岃繖閲岀洿鎺ユ敹鎺夈€?
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
        // 鍚庡彴閫€鍑哄叆鍙ｅ彲鑳戒粠娓呯悊鎵ц鍣ㄨ皟鐢紝Media3 浠嶉』鍦?applicationLooper 閲婃斁銆?
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

