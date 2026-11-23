package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.application.ForegroundPlaybackAttachment
import com.aritxonly.bdfun.core.domain.PlaybackProgressRepository
import com.aritxonly.bdfun.feature.video.presentation.VideoPlaybackPlatform

class PlayerEngineSettings(
    val backgroundPlay: () -> Boolean,
    val boostDefaultSpeed: () -> Float, val autoPlay: () -> Boolean,
    val resumePlayback: () -> Boolean, val autoPlayNextChanged: (Boolean) -> Unit,
    val loopModeChanged: (Int) -> Unit,
)
class PlayerEnginePorts(
    val page: PlayerPageHost, val background: ForegroundPlaybackAttachment,
    val progress: PlaybackProgressRepository, val exit: PlayerExitHooks,
    val diagnostics: PlayerDiagnostics, val settings: PlayerEngineSettings,
)

/** 原remember初始化键和读取时机由入口保留，app只提供既有平台和设置接线。 */
class PlayerEntryPorts(
    val createPlayback: () -> VideoPlaybackPlatform,
    val tracking: () -> com.aritxonly.bdfun.feature.video.application.PlaybackTrackingStateHolder,
    val defaultSpeed: () -> Float, val autoPlayNext: () -> Boolean,
    /** 上次的循环播放模式：新视频/新页面沿用，不再回到「不循环」。 */
    val loopMode: () -> Int,
    val sponsorSkipMode: () -> Int, val danmakuOn: () -> Boolean,
    val bluetoothLatency: @Composable () -> Int,
    val engine: PlayerEnginePorts,
    val render: @Composable (VideoPlaybackPlatform, VideoMediaInput, VideoPlayerPresentation, VideoPlayerActions, VideoPlayerUiStateHolder) -> PlayerRenderPorts,
    val hasNext: () -> Boolean,
)
