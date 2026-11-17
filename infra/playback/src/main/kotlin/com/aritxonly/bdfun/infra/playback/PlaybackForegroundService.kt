package com.aritxonly.bdfun.infra.playback

import com.aritxonly.bdfun.core.application.BackgroundPlaybackPort

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 后台播放保活：只负责前台通知和进程存活，真正的 ExoPlayer 仍由 [playback] 持有。
 * 这样退出视频页后系统还能正常播一镜到底收尾动画，声音也不会断。
 */
abstract class PlaybackForegroundService : Service() {
    protected abstract val playback: BackgroundPlaybackPort

    companion object {
        const val ACTION_START = "com.aritxonly.bdfun.BG_PLAYBACK_START"
        const val ACTION_TOGGLE = "com.aritxonly.bdfun.BG_PLAYBACK_TOGGLE"
        const val ACTION_PREV = "com.aritxonly.bdfun.BG_PLAYBACK_PREV"
        const val ACTION_NEXT = "com.aritxonly.bdfun.BG_PLAYBACK_NEXT"
        const val ACTION_STOP = "com.aritxonly.bdfun.BG_PLAYBACK_STOP"

        private const val CHANNEL_ID = "bg_playback"
        private const val NOTIFICATION_ID = 4401
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null
    private var mediaSession: MediaSessionCompat? = null
    private var artUrl: String? = null
    private var artBitmap: Bitmap? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        mediaSession = MediaSessionCompat(this, "BdFunBackgroundPlayback").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    if (!playback.playbackSnapshot.playWhenReady) playback.togglePlayPause()
                }

                override fun onPause() {
                    if (playback.playbackSnapshot.playWhenReady) playback.togglePlayPause()
                }

                override fun onSkipToNext() {
                    playback.next()
                }

                override fun onSkipToPrevious() {
                    playback.previous()
                }

                override fun onStop() {
                    playback.exit()
                }
            })
            isActive = true
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_TOGGLE -> playback.togglePlayPause()
            ACTION_PREV -> playback.previous()
            ACTION_NEXT -> playback.next()
            ACTION_STOP -> {
                playback.exit()
                stopSelf()
                return START_NOT_STICKY
            }
        }

        startForeground(NOTIFICATION_ID, buildNotification())
        if (ticker == null) {
            ticker = scope.launch {
                while (isActive) {
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
                    delay(1000L)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        ticker = null
        scope.cancel()
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** 拉取封面并裁成圆形，失败就保持上一张。 */
    private fun ensureArtwork(url: String) {
        if (url.isBlank() || url == artUrl) return
        artUrl = url
        scope.launch(Dispatchers.IO) {
            val result = runCatching {
                coil.Coil.imageLoader(this@PlaybackForegroundService).execute(
                    coil.request.ImageRequest.Builder(this@PlaybackForegroundService)
                        .data(url)
                        .size(512)
                        .allowHardware(false)
                        .build(),
                )
            }.getOrNull()
            val source = (result as? coil.request.SuccessResult)
                ?.drawable
                ?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap }
            if (source == null) return@launch
            val circular = mediaArtwork(source)
            withContext(Dispatchers.Main) {
                if (artUrl == url) {
                    artBitmap = circular
                    val manager =
                        getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
                }
            }
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台播放",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "视频在后台继续播放时的控制通知"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val coverUrl = playback.cover
        ensureArtwork(coverUrl)
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(
                this,
                0,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val title = playback.title.ifBlank { "正在播放" }
        val artist = playback.author.ifBlank { "bdFun" }
        val snapshot = playback.playbackSnapshot
        val playing = snapshot.playWhenReady && snapshot.phase != com.aritxonly.bdfun.core.application.PlaybackPhase.Ended
        val art = artBitmap
        val action = { name: String, code: Int ->
            PendingIntent.getService(
                this,
                code,
                Intent(this, javaClass).setAction(name),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        updateNotificationSession(title, artist, snapshot, art)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setLargeIcon(art)
            .setContentIntent(openIntent)
            .setDeleteIntent(action(ACTION_STOP, 13))
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_previous,
                "上一集",
                action(ACTION_PREV, 10),
            )
            .addAction(
                if (playing) {
                    android.R.drawable.ic_media_pause
                } else {
                    android.R.drawable.ic_media_play
                },
                if (playing) "暂停" else "播放",
                action(ACTION_TOGGLE, 11),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "下一集",
                action(ACTION_NEXT, 12),
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2),
            )
            .build()
    }
    private fun updateNotificationSession(title: String, artist: String, snapshot: com.aritxonly.bdfun.core.application.PlaybackSnapshot, art: Bitmap?) {
        mediaSession?.let { session ->
            session.setMetadata(
                MediaMetadataCompat.Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                    .putLong(
                        MediaMetadataCompat.METADATA_KEY_DURATION,
                        playback.durationMs,
                    )
                    .apply {
                        if (art != null) {
                            // 系统媒体控件显示圆形封面。
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, art)
                            putBitmap(MediaMetadataCompat.METADATA_KEY_ART, art)
                            putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, art)
                        }
                    }
                    .build(),
            )
            session.setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                            PlaybackStateCompat.ACTION_PAUSE or
                            PlaybackStateCompat.ACTION_PLAY_PAUSE or
                            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackStateCompat.ACTION_SEEK_TO or
                            PlaybackStateCompat.ACTION_STOP,
                    )
                    .setState(
                        mediaSessionPlaybackState(snapshot),
                        snapshot.positionMs,
                        snapshot.speed,
                    )
                    .build(),
            )
        }

    }

}
