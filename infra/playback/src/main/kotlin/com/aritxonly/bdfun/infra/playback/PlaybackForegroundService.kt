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

    /** 当前挂在前台服务上的通知 id：页面会话活着时复用它那条，否则用服务自己的。 */
    private var notificationId = NOTIFICATION_ID

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

        PlaybackNotificationArbiter.registerOwnerChangedListener(::syncForegroundNotification)
        val page = PlaybackNotificationArbiter.pageSession
        if (page != null) {
            // 视频页还活着：直接把它那条媒体通知当前台通知，不再另发一条控制栏。
            page.islandExtras = ::currentIslandExtras
            notificationId = page.notificationId
            startForeground(notificationId, page.currentNotification())
        } else {
            notificationId = NOTIFICATION_ID
            startForeground(NOTIFICATION_ID, buildNotification())
        }
        if (ticker == null) {
            ticker = scope.launch {
                var lastSignature: String? = null
                while (isActive) {
                    lastSignature = tickNotification(lastSignature)
                    delay(1000L)
                }
            }
        }
        return START_STICKY
    }

    /** 只有真的变了才重建通知：标题/播放态/队列/封面。进度由系统按 PlaybackState 自己推算，不用每秒重画。 */
    private fun notificationSignature(): String {
        val snapshot = playback.playbackSnapshot
        return listOf(
            playback.title,
            playback.author,
            playback.cover,
            playback.playing,
            snapshot.phase,
            snapshot.speed,
            artUrl.orEmpty(),
        ).joinToString("|")
    }

    /**
     * 每秒刷新通知：页面会话在时由它自更新，只在归属变化时重新挂前台通知；
     * 归属回自己后再按签名去重刷新，避免上一轮的签名残留导致不刷新。
     */
    private fun tickNotification(lastSignature: String?): String? {
        val page = PlaybackNotificationArbiter.pageSession
        if (page != null) {
            if (notificationId != page.notificationId) syncForegroundNotification()
            return null
        }
        if (notificationId != NOTIFICATION_ID) {
            syncForegroundNotification()
            return null
        }
        val signature = notificationSignature()
        if (signature == lastSignature) return lastSignature
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
        return signature
    }

    /** 通知归属变化（页面会话出现或消失）：把前台通知切到当前拥有者那条。 */
    private fun syncForegroundNotification() {
        val page = PlaybackNotificationArbiter.pageSession
        if (page != null) {
            page.islandExtras = ::currentIslandExtras
            if (notificationId == page.notificationId) return
            val previous = notificationId
            notificationId = page.notificationId
            // 先挂新的再撤旧的：反过来做的中间会有一小段通知栏里一条都没有。
            runCatching { startForeground(notificationId, page.currentNotification()) }
            cancelNotification(previous)
        } else {
            if (notificationId == NOTIFICATION_ID) return
            val previous = notificationId
            notificationId = NOTIFICATION_ID
            // 页面会话释放时为了不让前台通知空掉而没有自己取消，这里由接管方清掉。
            runCatching { startForeground(NOTIFICATION_ID, buildNotification()) }
            cancelNotification(previous)
        }
    }

    private fun cancelNotification(id: Int) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        runCatching { manager.cancel(id) }
    }

    override fun onDestroy() {
        ticker?.cancel()
        ticker = null
        scope.cancel()
        PlaybackNotificationArbiter.unregisterOwnerChangedListener()
        PlaybackNotificationArbiter.pageSession?.islandExtras = null
        runCatching {
            mediaSession?.isActive = false
            mediaSession?.release()
        }
        mediaSession = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** 前后台均提交完整原图，由系统媒体面板决定显示形状。 */
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
            withContext(Dispatchers.Main) {
                if (artUrl == url) {
                    artBitmap = source
                    // 通知已被页面会话接管时不能再发服务自己那条，否则又多一条控制栏。
                    if (PlaybackNotificationArbiter.pageSession == null) {
                        val manager =
                            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        runCatching { manager.notify(NOTIFICATION_ID, buildNotification()) }
                    }
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

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
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
        // HyperOS 超级岛：带上分享载荷后可以把媒体通知拖进聊天窗口直接分享。
        currentIslandExtras()?.let(builder::addExtras)
        return builder.build()
    }

    /** 超级岛分享载荷；页面会话接管通知时也复用这里，保证两种归属能力一致。 */
    private fun currentIslandExtras(): android.os.Bundle? = xiaomiMediaIslandExtras(
        playback.title.ifBlank { "正在播放" },
        playback.author.ifBlank { "bdFun" },
        playback.shareUrl,
    )
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
                            // 与页面会话使用相同原图，避免两种封面输入交替。
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
