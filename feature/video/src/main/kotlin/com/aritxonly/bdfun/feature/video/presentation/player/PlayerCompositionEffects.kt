package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform
import com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder

@Composable
fun BindPlayerInitialEffects(
    playback: VideoPlaybackPlatform, media: VideoMediaInput, ui: VideoPlayerUiStateHolder, ports: PlayerEnginePorts,
) {
    val log = ports.diagnostics.log
    val bvid = media.bvid
    val info = media.info
    val quality = media.quality
    var usingFallback by ui.usingFallback
    var usingOriginal by ui.usingOriginal
    BindPlayerLifecycle(playback, ports.page, ports.settings.backgroundPlay)
    BindPlayerBuffering(playback, ui)
    LaunchedEffect(info, quality) {
        usingFallback = false
        usingOriginal = false
        log(
            "PLAYER",
            "quality-change bvid=$bvid q=${quality?.id} hasInfo=${info != null} " +
                "qualities=${info?.qualities?.size ?: 0}",
        )
    }
}

@Composable
fun BindPlayerPresentationSetup(
    media: VideoMediaInput, presentation: VideoPlayerPresentation, ui: VideoPlayerUiStateHolder, ports: PlayerEnginePorts,
) {
    val bvid = media.bvid
    val fullscreenInfo = presentation.fullscreenInfo
    val fullscreen = presentation.fullscreen
    // 全屏新 UI 的本地状态：二级面板、听视频、镜像、循环、画面尺寸、定时关闭等
    BindPlayerOrientation(fullscreen, ui, ports.page)
    var fsAutoPlayNext by ui.fsAutoPlayNext
    LaunchedEffect(fsAutoPlayNext) {
        ports.settings.autoPlayNextChanged(fsAutoPlayNext)
    }
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    LaunchedEffect(bvid, fullscreenInfo.currentCid) {
        videoScale = 1f
        videoRotation = 0f
        videoOffset = Offset.Zero
    }

}

@Composable
fun BindPlayerEngineEffects(
    playback: VideoPlaybackPlatform, observed: PlayerObservedStateHolder,
    media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, ui: VideoPlayerUiStateHolder, tracking: PlaybackTrackingStateHolder, ports: PlayerEnginePorts,
) {
    val playing by observed.playing
    BindPlayerMediaBridge(playback, media, presentation, actions, ui, ports.background, ports.settings.boostDefaultSpeed)
    BindPlayerControlVisibility(playback, presentation.fullscreen, playing, ui, ports.page)
    BindPlayerObserver(playback, media, actions, ui, ports.page, ports.progress, ports.exit, ports.diagnostics)
    BindPlayerSource(playback, media, presentation, ui, ports.diagnostics, ports.settings.autoPlay, ports.progress, ports.settings.resumePlayback)
    BindPlayerProgressTracking(playback, media.bvid, ui, tracking, ports.page, ports.progress, ports.settings.resumePlayback)
}

