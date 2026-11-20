package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun VideoPlayerEntry(
    media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, modifier: Modifier = Modifier, ports: PlayerEntryPorts,
) {
    val ui = remember {
        VideoPlayerUiStateHolder(ports.defaultSpeed(), ports.autoPlayNext(), presentation.fullscreen)
    }
    val playback = remember {
        ports.createPlayback()
    }
    val observed = rememberPlayerObservedState(playback)
    BindPlayerInitialEffects(playback, media, ui, ports.engine)
    val tracking = remember { ports.tracking() }
    val sponsor = rememberPlayerSponsor(media.bvid, tracking, observed, ports.sponsorSkipMode)
    BindPlayerPresentationSetup(media, presentation, ui, ports.engine)
    val latencyMs = ports.bluetoothLatency()
    BindPlayerContinuation(playback, presentation.fullscreenInfo, actions.transport.onPageSelected, ui,
        ports.engine.page, ports.hasNext, ports.engine.diagnostics.toast)
    BindPlayerEngineEffects(playback, observed, media, presentation, actions, ui, tracking, ports.engine)
    val audioOnly by ui.fsAudioOnly
    val danmakuVisible = !audioOnly && ports.danmakuOn() && media.danmaku.isNotEmpty()
    val playing by observed.playing
    BindPlayerHotDanmaku(playback, danmakuVisible, ui)
    BindPlayerDanmakuClock(playback, playing, danmakuVisible, ui)
    val isVertical = com.aritxonly.bdfun.core.domain.isVerticalVideoAspect(presentation.fullscreenInfo.videoAspectRatio)
    BindPlayerSystemBars(presentation.fullscreen, isVertical, presentation.immersiveStatusBar, ui)
    BindPlayerHdrWindow(media.quality, presentation.fullscreen)
    VideoPlayerLayout(playback, observed, media, presentation, actions, ui, danmakuVisible, latencyMs, sponsor, modifier,
        ports.render(playback, media, presentation, actions, ui))
}
