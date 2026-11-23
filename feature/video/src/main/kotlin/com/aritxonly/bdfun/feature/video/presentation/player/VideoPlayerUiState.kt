package com.aritxonly.bdfun.ui

import android.content.pm.ActivityInfo
import com.aritxonly.bdfun.core.application.ForegroundMediaSession
import com.aritxonly.bdfun.feature.video.presentation.VideoOutput
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job

/** 原 remember(Unit) 的瞬时 UI/资源引用；状态对象与播放器组合同生命周期。 */
class VideoPlayerUiStateHolder(
    initialSpeed: Float, initialAutoPlayNext: Boolean, initialLoopMode: Int, initialFullscreen: Boolean,
) {
    val danmakuHit = PlayerDanmakuHit()

    val showBuffering = mutableStateOf(false)
    val usingFallback = mutableStateOf(false)
    val usingOriginal = mutableStateOf(false)
    val lastProgressSaveAt = mutableLongStateOf(0L)
    val controlsVisible = mutableStateOf(false)
    val speed = mutableFloatStateOf(initialSpeed)
    val speedMenu = mutableStateOf(false)
    val qualityMenu = mutableStateOf(false)
    val fsPanel = mutableStateOf(FullscreenPanel.None)
    val fsInputOpen = mutableStateOf(false)
    val fsLocked = mutableStateOf(false)
    val fsLockHint = mutableStateOf(false)
    val fsAudioOnly = mutableStateOf(false)
    val halfMoreOpen = mutableStateOf(false)
    val fsAutoPlayNext = mutableStateOf(initialAutoPlayNext)
    val fsLoopMode = mutableIntStateOf(initialLoopMode.coerceIn(0, 2))
    val fsMirror = mutableStateOf(false)
    val fsResizeMode = mutableIntStateOf(0)
    val fsColorAssist = mutableIntStateOf(0)
    val fsSubtitlesOn = mutableStateOf(true)
    val fsSleepAt = mutableLongStateOf(0L)
    val fsSeekLook = mutableFloatStateOf(0f)
    val fsScrubbing = mutableStateOf(false)
    val fsBarPreviewMs = mutableLongStateOf(-1L)
    val boosting = mutableStateOf(false)
    val boostSpeed = mutableFloatStateOf(2.5f)
    val videoScale = mutableFloatStateOf(1f)
    val videoRotation = mutableFloatStateOf(0f)
    val videoOffset = mutableStateOf(Offset.Zero)
    val frameMs = mutableLongStateOf(0L)
    /** 常显进度条用的位置/总长：每秒采一次，只在绘制阶段读取，不触发播放器重组。 */
    val progressPositionMs = mutableLongStateOf(0L)
    val progressDurationMs = mutableLongStateOf(0L)
    val preparedMediaKey = mutableStateOf<String?>(null)
    val renderedMediaKey = mutableStateOf<String?>(null)
    val interactionTick = mutableIntStateOf(0)
    val volumeTip = mutableStateOf<Float?>(null)
    val brightnessTip = mutableStateOf<Float?>(null)
    val dragBase = mutableFloatStateOf(0.5f)
    val seekPreviewMs = mutableLongStateOf(-1L)
    val seekBaseMs = mutableLongStateOf(0L)
    val playerViewRef = mutableStateOf<VideoOutput?>(null)
    val mediaSession = mutableStateOf<ForegroundMediaSession?>(null)
    val lastFullscreenForControls = mutableStateOf(initialFullscreen)
    val hotDanmaku = mutableStateOf(com.aritxonly.bdfun.core.domain.HotDanmakuWindow())
    val inlineOrientation = mutableIntStateOf(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
}
