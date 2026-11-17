package com.aritxonly.bdfun.infra.playback

import com.aritxonly.bdfun.core.application.ForegroundMediaSession
import com.aritxonly.bdfun.core.application.MediaControlsRegistry
import com.aritxonly.bdfun.core.application.MediaQueueEntry

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media.app.NotificationCompat.MediaStyle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.aritxonly.bdfun.core.application.PlaybackController
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

/**
 * 系统媒体控件：锁屏 / 通知栏 / 蓝牙耳机都能控制播放。
 * 用平台自带的 MediaSessionCompat + MediaStyle 通知，不额外起 Service（播放仍在前台 Activity）。
 */
class MediaPlaybackSession(
    private val context: Context,
    private val registry: MediaControlsRegistry,
    private val actionReceiver: Class<out BroadcastReceiver>,
    private val playback: PlaybackController,
    private val title: String,
    private val artist: String,
    private val artworkUrl: String? = null,
    private val onQueueSelected: (Int) -> Unit = {},
) : ForegroundMediaSession {
    companion object {
        const val CHANNEL_ID = "teak_playback"
        const val NOTIFICATION_ID = 4201
        const val ACTION_TOGGLE = "com.aritxonly.bdfun.MEDIA_TOGGLE"
        const val ACTION_REWIND = "com.aritxonly.bdfun.MEDIA_REWIND"
        const val ACTION_FORWARD = "com.aritxonly.bdfun.MEDIA_FORWARD"
        const val ACTION_PREV = "com.aritxonly.bdfun.MEDIA_PREV"
        const val ACTION_NEXT = "com.aritxonly.bdfun.MEDIA_NEXT"
    }

    private val manager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var lastNotifyAt = 0L
    private var lastPlaying: Boolean? = null
    private var released = false
    private var artwork: android.graphics.Bitmap? = null
    private var lastPosition = 0L
    private var lastDuration = 0L
    private var queue: List<MediaQueueEntry> = emptyList()
    private var queueIndex = -1
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.IO + kotlinx.coroutines.SupervisorJob(),
    )

    private val session = MediaSessionCompat(context, "TeakBili").apply {
        setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                playback.play()
            }

            override fun onPause() {
                playback.pause()
            }

            override fun onSeekTo(pos: Long) {
                playback.seekTo(pos.coerceAtLeast(0L))
            }

            override fun onFastForward() {
                seekBy(10_000)
            }

            override fun onRewind() {
                seekBy(-10_000)
            }

            override fun onSkipToNext() {
                if (queueIndex + 1 in queue.indices) {
                    onQueueSelected(queueIndex + 1)
                }
            }

            override fun onSkipToPrevious() {
                if (queueIndex - 1 in queue.indices) {
                    onQueueSelected(queueIndex - 1)
                }
            }

            override fun onSkipToQueueItem(id: Long) {
                if (id.toInt() in queue.indices) {
                    onQueueSelected(id.toInt())
                }
            }

            override fun onStop() {
                playback.pause()
            }
        })
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
        )
        isActive = true
    }

    init {
        createChannel()
        session.setMetadata(
            MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .build(),
        )
        registry.attach(this)
        loadArtwork(artworkUrl)
    }

    /** 更新系统媒体控件的选集队列与当前集。 */
    override fun updateQueue(entries: List<MediaQueueEntry>, currentIndex: Int) {
        if (released) return
        queue = entries
        queueIndex = currentIndex
        val items = entries.mapIndexed { index, entry ->
            MediaSessionCompat.QueueItem(
                MediaDescriptionCompat.Builder()
                    .setMediaId(entry.mediaId)
                    .setTitle(entry.title)
                    .setSubtitle(entry.subtitle)
                    .build(),
                index.toLong(),
            )
        }
        session.setQueue(items)
        session.setQueueTitle("选集")
        session.setActive(true)
    }

    /** 异步拉取视频封面，塞进媒体控件与通知。 */
    private fun loadArtwork(url: String?) {
        if (url.isNullOrBlank()) return
        scope.launch {
            val result = runCatching {
                coil.Coil.imageLoader(context).execute(
                    coil.request.ImageRequest.Builder(context)
                        .data(url)
                        .size(512)
                        .allowHardware(false)
                        .build(),
                )
            }.getOrNull()
            val bitmap = (result as? coil.request.SuccessResult)
                ?.drawable
                ?.let { (it as? android.graphics.drawable.BitmapDrawable)?.bitmap }
            if (bitmap != null && !released) {
                artwork = mediaArtwork(bitmap)
                updateMetadata()
                notifyState(lastPlaying == true, lastPosition, lastDuration)
            }
        }
    }

    private fun updateMetadata() {
        val builder = MediaMetadataCompat.Builder()
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, lastDuration)
        artwork?.let {
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, it)
            builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, it)
        }
        session.setMetadata(builder.build())
    }

    override fun toggle() {
        if (playback.refresh().playing) playback.pause() else playback.play()
    }

    override fun seekBy(deltaMs: Long) {
        val duration = playback.refresh().durationMs.coerceAtLeast(0L)
        val target = (playback.refresh().positionMs + deltaMs).coerceIn(0L, duration)
        playback.seekTo(target)
    }

    override fun next() {
        if (queueIndex + 1 in queue.indices) onQueueSelected(queueIndex + 1)
    }

    override fun previous() {
        if (queueIndex - 1 in queue.indices) onQueueSelected(queueIndex - 1)
    }

    override fun onPlayerState(
        playing: Boolean,
        buffering: Boolean,
        positionMs: Long,
        durationMs: Long,
        speed: Float,
    ) {
        if (released) return
        val state = when {
            buffering -> PlaybackStateCompat.STATE_BUFFERING
            playing -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        if (durationMs > 0L && durationMs != lastDuration) {
            lastDuration = durationMs
            updateMetadata()
        }
        session.setPlaybackState(
            PlaybackStateCompat.Builder()
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_PLAY_PAUSE or
                        PlaybackStateCompat.ACTION_SEEK_TO or
                        PlaybackStateCompat.ACTION_FAST_FORWARD or
                        PlaybackStateCompat.ACTION_REWIND or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                        PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM,
                )
                .setState(state, positionMs, if (speed > 0f) speed else 1f)
                .build(),
        )
        lastPosition = positionMs
        val now = System.currentTimeMillis()
        val playingChanged = lastPlaying != playing
        if (!playingChanged && now - lastNotifyAt < 1000L) return
        lastPlaying = playing
        lastNotifyAt = now
        notifyState(playing, positionMs, durationMs)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "播放控制",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "视频播放的媒体控制"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun actionIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, actionReceiver).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun notifyState(playing: Boolean, positionMs: Long, durationMs: Long) {
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let {
                PendingIntent.getActivity(
                    context,
                    0,
                    it,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(contentIntent)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setOngoing(playing)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setLargeIcon(artwork)
            .addAction(
                android.R.drawable.ic_media_previous,
                "上一集",
                actionIntent(ACTION_PREV, 10),
            )
            .addAction(
                android.R.drawable.ic_media_rew,
                "后退 10 秒",
                actionIntent(ACTION_REWIND, 11),
            )
            .addAction(
                if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (playing) "暂停" else "播放",
                actionIntent(ACTION_TOGGLE, 12),
            )
            .addAction(
                android.R.drawable.ic_media_ff,
                "前进 10 秒",
                actionIntent(ACTION_FORWARD, 13),
            )
            .addAction(
                android.R.drawable.ic_media_next,
                "下一集",
                actionIntent(ACTION_NEXT, 14),
            )
            .setStyle(
                MediaStyle()
                    .setMediaSession(session.sessionToken)
                    .setShowActionsInCompactView(1, 2, 3),
            )
        if (durationMs > 0L) {
            builder.setProgress(
                (durationMs / 1000).toInt().coerceAtLeast(1),
                (positionMs / 1000).toInt().coerceIn(0, (durationMs / 1000).toInt()),
                false,
            )
        }
        runCatching { manager.notify(NOTIFICATION_ID, builder.build()) }
    }

    override fun release() {
        if (released) return
        released = true
        scope.cancel()
        registry.detach(this)
        runCatching { manager.cancel(NOTIFICATION_ID) }
        runCatching {
            session.isActive = false
            session.release()
        }
    }
}
