package com.aritxonly.bdfun.infra.playback

import android.os.SystemClock
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener

/** 由 app 仅在 debug 注入；不记录 URL、请求头、账号和媒体标识。 */
internal class PlaybackDiagnostics(context: android.content.Context, private val write: (String) -> Unit) : AnalyticsListener {
    private val power = context.getSystemService(android.os.PowerManager::class.java)
    private var lastSampleMs = 0L
    private var lastBandwidthMs = 0L

    fun sample(player: ExoPlayer) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastSampleMs < 5_000) return
        lastSampleMs = now
        write("sample state=${player.playbackState} intent=${player.playWhenReady} " +
            "playing=${player.isPlaying} loading=${player.isLoading} position=${player.currentPosition} " +
            "bufferMs=${player.totalBufferedDuration} speed=${player.playbackParameters.speed} " +
            "suppression=${player.playbackSuppressionReason} screen=${power.isInteractive} " +
            "idle=${power.isDeviceIdleMode} powerSave=${power.isPowerSaveMode}")
    }

    override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int,
        totalBytesLoaded: Long, bitrateEstimate: Long) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBandwidthMs < 5_000) return
        lastBandwidthMs = now
        write("transfer elapsedMs=$totalLoadTimeMs bytes=$totalBytesLoaded estimate=$bitrateEstimate")
    }

    override fun onAudioUnderrun(eventTime: AnalyticsListener.EventTime, bufferSize: Int,
        bufferSizeMs: Long, elapsedSinceLastFeedMs: Long) {
        write("audioUnderrun bytes=$bufferSize bufferMs=$bufferSizeMs sinceFeedMs=$elapsedSinceLastFeedMs")
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        write("phase=$state")
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: androidx.media3.common.PlaybackException) {
        write("error=${error.errorCodeName} cause=${error.cause?.javaClass?.simpleName}")
    }
}
