package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.common.formatDuration
import com.aritxonly.bdfun.core.application.ForegroundPlaybackAttachment
import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.MediaQueueEntry
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

@Composable
fun BindPlayerMediaBridge(
    playback: VideoPlaybackPlatform,
    media: VideoMediaInput,
    presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder, background: ForegroundPlaybackAttachment, boostDefaultSpeed: () -> Float,
) {
    val context = LocalContext.current
    val title = media.title
    val author = media.author
    val cover = media.cover
    val fullscreenInfo = presentation.fullscreenInfo
    val onPageSelected = actions.transport.onPageSelected
    var mediaSession by ui.mediaSession
    val latestPages by rememberUpdatedState(fullscreenInfo.pages)
    DisposableEffect(title, author) {
        val session = playback.createMediaSession(
            context = context,
            title = title,
            artist = author,
            artworkUrl = cover,
            onQueueSelected = { index ->
                latestPages.getOrNull(index)?.let(onPageSelected)
            },
        )
        mediaSession = session
        onDispose {
            session.release()
            if (mediaSession === session) mediaSession = null
        }
    }

    // 系统媒体控件的进度条/选集：队列和当前索引随分 P 切换更新。
    LaunchedEffect(fullscreenInfo.pages, fullscreenInfo.currentCid, mediaSession) {
        val pages = fullscreenInfo.pages
        if (pages.isEmpty()) return@LaunchedEffect
        val entries = pages.mapIndexed { index, page ->
            MediaQueueEntry(
                mediaId = page.cid.toString(),
                title = page.part.ifBlank { "第 ${index + 1} 集" },
                subtitle = formatDuration(page.duration),
            )
        }
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        mediaSession?.updateQueue(entries, index)
    }

    BindPlayerBackgroundBridge(playback, presentation, actions, ui, background, boostDefaultSpeed)
}

@Composable
private fun BindPlayerBackgroundBridge(
    playback: PlaybackController,
    presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions,
    ui: VideoPlayerUiStateHolder, background: ForegroundPlaybackAttachment, boostDefaultSpeed: () -> Float,
) {
    val context = LocalContext.current
    val fullscreenInfo = presentation.fullscreenInfo
    val fullscreen = presentation.fullscreen
    val onPageSelected = actions.transport.onPageSelected
    val speed by ui.speed
    var boostSpeed by ui.boostSpeed
    var boosting by ui.boosting
    // 底栏唱片：把当前播放器接到进程级桥上。唱片在首页时直接调这里的动作。
    val bgToken = remember { Any() }
    LaunchedEffect(fullscreen) { background.updateFullscreen(bgToken, fullscreen) }
    DisposableEffect(bgToken) { onDispose { background.updateFullscreen(bgToken, false) } }
    val bgPages by rememberUpdatedState(fullscreenInfo.pages)
    val bgCurrentCid by rememberUpdatedState(fullscreenInfo.currentCid)
    val bgPageSelected by rememberUpdatedState(onPageSelected)
    val bgSpeed by rememberUpdatedState(speed)
    DisposableEffect(bgToken) {
        background.attach(
            bgToken, playback,
            object : ForegroundPlaybackAttachment.Actions {
                override fun next() {
                    val pages = bgPages
                    val index = pages.indexOfFirst { it.cid == bgCurrentCid }
                    pages.getOrNull(index + 1)?.let(bgPageSelected)
                }

                override fun previous() {
                    val pages = bgPages
                    val index = pages.indexOfFirst { it.cid == bgCurrentCid }
                    if (index > 0) pages.getOrNull(index - 1)?.let(bgPageSelected)
                }

                override fun togglePlayPause() {
                    if (playback.refresh().playWhenReady) playback.pause() else playback.play()
                }

                override fun boostStart() {
                    if (!playback.refresh().playing) return
                    boostSpeed = boostDefaultSpeed()
                    playback.setSpeed(boostSpeed)
                    boosting = true
                }

                override fun boostEnd() {
                    if (!boosting) return
                    playback.setSpeed(bgSpeed)
                    boosting = false
                }

                override fun seekTo(ms: Long) {
                    playback.seekTo(ms)

                }

                override fun exit() {
                    playback.pause()
                    (context as? Activity)?.finish()
                }
            },
        )
        onDispose { background.detach(bgToken) }
    }

}
