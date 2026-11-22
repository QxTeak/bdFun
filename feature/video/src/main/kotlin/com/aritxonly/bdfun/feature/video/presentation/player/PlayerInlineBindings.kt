package com.aritxonly.bdfun.ui


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayerInlineControls(
    playback: com.aritxonly.bdfun.core.application.PlaybackController,
    observed: PlayerObservedStateHolder,
    presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions,
    fsActions: FullscreenActions,
    ui: VideoPlayerUiStateHolder, ports: PlayerRenderPorts,
) {
    val playing by observed.playing
    val positionMs by observed.positionMs
    val durationMs by observed.durationMs
    val bufferedMs by observed.bufferedMs
    val fullscreen = presentation.fullscreen
    val pictureInPicture = presentation.pictureInPicture
    val immersiveStatusBar = presentation.immersiveStatusBar
    var fsAudioOnly by ui.fsAudioOnly
    var controlsVisible by ui.controlsVisible
    var seekPreviewMs by ui.seekPreviewMs
    var fsScrubbing by ui.fsScrubbing
    var fsSeekLook by ui.fsSeekLook
    val controlsAlpha = if (controlsVisible && !pictureInPicture) 1f else 0f
    // 半屏播放器：参考官方横屏的紧凑条，不再使用竖屏那套完整控件
    if (!fullscreen && !fsAudioOnly && !pictureInPicture) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = if (controlsVisible) 0.dp else 10_000.dp)
                .graphicsLayer { alpha = controlsAlpha },
        ) {
            InlineCompactChrome(
                state = InlineChromeState(
                    playing = playing,
                    positionMs = if (controlsVisible) {
                        if (seekPreviewMs >= 0L) seekPreviewMs else positionMs
                    } else {
                        0L
                    },
                    durationMs = if (controlsVisible) durationMs else 0L,
                    bufferedMs = if (controlsVisible) bufferedMs else 0L,
                    scrubbing = controlsVisible && fsScrubbing,
                    seekLook = if (controlsVisible) fsSeekLook else 0f,
                    immersiveStatusBar = immersiveStatusBar,
                ),
                actions = inlineChromeActions(playback, observed, actions, fsActions, ui),
                appearance = ports.inlineAppearance(),
            )
        }
    }

}

@Composable
internal fun PlayerInlineAudio(
    playback: com.aritxonly.bdfun.core.application.PlaybackController,
    observed: PlayerObservedStateHolder,
    presentation: VideoPlayerPresentation,
    fsActions: FullscreenActions,
    ui: VideoPlayerUiStateHolder, ports: PlayerRenderPorts,
) {
    val playing by observed.playing
    val positionMs by observed.positionMs
    val durationMs by observed.durationMs
    val fullscreen = presentation.fullscreen
    val fullscreenInfo = presentation.fullscreenInfo
    var fsAudioOnly by ui.fsAudioOnly
    fun touch() { ui.interactionTick.intValue++ }
    // 半屏听视频：不进入全屏也能用，播放器区域换成音频模式。
    if (!fullscreen && fsAudioOnly) {
        val pages = fullscreenInfo.pages
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        HalfScreenAudioMode(
            appearance = ports.inlineAppearance(),
            coverImage = ports.cover,
            title = fullscreenInfo.title,
            author = fullscreenInfo.author,
            cover = fullscreenInfo.cover,
            playing = playing,
            positionMs = positionMs,
            durationMs = durationMs,
            onExit = { fsAudioOnly = false },
            onPlayPause = { touch(); togglePlay(playback, playing) },
            onPrevious = {
                touch()
                pages.getOrNull(index - 1)?.let(fsActions.onPageSelected)
            },
            onNext = {
                touch()
                pages.getOrNull(index + 1)?.let(fsActions.onPageSelected)
            },
            hasPrevious = index > 0,
            hasNext = index >= 0 && index < pages.lastIndex,
        )
    }

}


private fun inlineChromeActions(
    playback: com.aritxonly.bdfun.core.application.PlaybackController,
    observed: PlayerObservedStateHolder,
    actions: VideoPlayerActions,
    fsActions: FullscreenActions,
    ui: VideoPlayerUiStateHolder,
): InlineChromeActions {
    val playing by observed.playing
    val onBackClick = actions.navigation.onBackClick
    val onHomeClick = actions.navigation.onHomeClick
    val onToggleFullscreen = actions.navigation.onToggleFullscreen
    var fsScrubbing by ui.fsScrubbing
    var fsSeekLook by ui.fsSeekLook
    var fsAudioOnly by ui.fsAudioOnly
    var halfMoreOpen by ui.halfMoreOpen
    fun touch() { ui.interactionTick.intValue++ }
    return InlineChromeActions(
        onBack = { touch(); onBackClick() },
        onHome = { touch(); onHomeClick() },
        onRewind6 = {
            touch()
            val target = (playback.refresh().positionMs - 6_000L).coerceAtLeast(0L)
            playback.seekTo(target)

        },
        onPlayPause = {
            touch()
            togglePlay(playback, playing)
        },
        onSeek = { target ->
            touch()
            playback.seekTo(target)

        },
        onScrubStart = { fsScrubbing = true },
        onScrubEnd = {
            fsScrubbing = false
            fsSeekLook = 0f
        },
        onScrubLook = { fsSeekLook = it },
        onFullscreen = {
            touch()
            onToggleFullscreen()
        },
        onListen = {
            touch()
            // 点「听视频」就是为了后台听：顺手把后台播放打开，
            // 不用再进「更多」里单独开一次「后台听视频」。
            fsActions.onBackgroundPlayChanged(true)
            fsAudioOnly = true
        },
        onCast = { touch(); fsActions.onCast() },
        onMore = { touch(); halfMoreOpen = true },
    )
}
