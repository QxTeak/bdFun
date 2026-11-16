package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.application.PlaybackController

@Composable
internal fun playerInteractionModifier(
    playback: PlaybackController,
    ui: VideoPlayerUiStateHolder,
    fullscreen: Boolean,
    page: PlayerPageHost, options: PlayerGestureOptions,
    onToggleFullscreen: () -> Unit,
): Modifier {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val density = LocalDensity.current
    val controlTopPx = with(density) { (if (fullscreen) 64.dp else 32.dp).toPx() }
    val controlBottomPx = with(density) { (if (fullscreen) 118.dp else 42.dp).toPx() }
    val panel by ui.fsPanel
    val input by ui.fsInputOpen
    val audioOnly by ui.fsAudioOnly
    val locked by ui.fsLocked
    return if (panel == FullscreenPanel.None && !input && !audioOnly && !locked) {
        Modifier.playerGestures(
            VideoPlayerGestureBindings(context, audio, playback, ui, page, options, controlTopPx, controlBottomPx, onToggleFullscreen).create(),
            options.verticalGesturesEnabled,
        )
    } else {
        Modifier
    }
}

private class VideoPlayerGestureBindings(
    private val context: Context,
    private val audio: AudioManager,
    private val playback: PlaybackController,
    private val ui: VideoPlayerUiStateHolder,
    private val page: PlayerPageHost, private val options: PlayerGestureOptions,
    private val controlTopPx: Float,
    private val controlBottomPx: Float,
    private val onToggleFullscreen: () -> Unit,
) {
    private val activity = context as? Activity
    private val PlayerUiState get() = page
    private val playing get() = playback.state.value.playing
    private var controlsVisible by ui.controlsVisible
    private var boostSpeed by ui.boostSpeed
    private var boosting by ui.boosting
    private var speed by ui.speed
    private var fsSeekLook by ui.fsSeekLook
    private var fsScrubbing by ui.fsScrubbing
    private var seekBaseMs by ui.seekBaseMs
    private var seekPreviewMs by ui.seekPreviewMs
    private var dragBase by ui.dragBase
    private var brightnessTip by ui.brightnessTip
    private var volumeTip by ui.volumeTip

    private fun touch() { ui.interactionTick.intValue++ }

    private val onTap: () -> Unit = {
        controlsVisible = !controlsVisible
        touch()
    }

    private val onDoubleTap: () -> Unit = {
        togglePlay(playback, playing)
        touch()
    }

    private val onLongPressStart: () -> Unit = {
        if (playing) {
            boostSpeed = options.boostDefaultSpeed()
            // 先让播放器收到倍速，再触发界面状态，减少“先震动、后加速”的感觉。
            playback.setSpeed(boostSpeed)
            boosting = true
            // 触发加速的瞬间震一下，确认手势已生效
            if (options.hapticsEnabled()) buzzDevice(context)
            // 面板/控件开着时直接收起，把画面让出来
            controlsVisible = false
            PlayerUiState.controlsVisible = false
        }
    }

    private val onLongPressEnd: () -> Unit = {
        playback.setSpeed(speed)
        boosting = false
    }

    private val onDragStart: (Boolean, Boolean, Boolean) -> Unit = { leftSide, horizontal, verticalAllowed ->
        if (horizontal) {
            fsSeekLook = 0f
            fsScrubbing = true
            seekBaseMs = playback.refresh().positionMs
            seekPreviewMs = seekBaseMs
            controlsVisible = true
            PlayerUiState.controlsVisible = true
            touch()
        } else if (verticalAllowed) {
            dragBase = if (leftSide) {
                currentBrightness(activity)
            } else {
                currentVolumeRatio(audio)
            }
            if (leftSide) {
                brightnessTip = dragBase
            } else {
                volumeTip = dragBase
            }
        }
    }

    private val onDrag: (Boolean, Float) -> Unit = { leftSide, totalDy ->
        val next = (dragBase - totalDy / 1200f).coerceIn(0f, 1f)
        if (leftSide) {
            applyBrightness(activity, next)
            brightnessTip = next
        } else {
            setVolumeRatio(audio, next)
            volumeTip = next
        }
    }

    private val onHorizontalDrag: (Float) -> Unit = { fraction ->
        fsSeekLook = (fraction / 0.04f).coerceIn(-1f, 1f)
        val duration = playback.refresh().durationMs.coerceAtLeast(0L)
        if (duration > 0) {
            val delta = fraction * duration * 0.2f
            seekPreviewMs = (seekBaseMs + delta.toLong())
                .coerceIn(0L, duration)
        }
    }

    private val onDragEnd: () -> Unit = {
        fsSeekLook = 0f
        if (fsScrubbing) {
            fsScrubbing = false
            controlsVisible = false
            PlayerUiState.controlsVisible = false
        }
        brightnessTip = null
        volumeTip = null
        if (seekPreviewMs >= 0L) {
            playback.seekTo(seekPreviewMs)

            seekPreviewMs = -1L
        }
    }

    // 控件可见时，顶部标题栏和底部控制条交给控件自己处理，
    // 中间视频区域仍然可以单击/双击/拖动。

    private val ignoreDown: (Offset, Float) -> Boolean = { position, height ->
        controlsVisible &&
            (position.y < controlTopPx ||
                position.y > height - controlBottomPx)
    }

    private val centerSwipeEnabled: Boolean = options.centerSwipeEnabled()

    private val onCenterSwipe: () -> Unit = { onToggleFullscreen() }

    private val boostSwipeEnabled: Boolean = options.boostSwipeEnabled()

    private val onBoostSwipe: (Float) -> Unit = { fraction ->
        val base = options.boostDefaultSpeed()
        val next = (base + fraction * 6f).coerceIn(1f, 5f)
        boostSpeed = next
        playback.setSpeed(next)
    }

    fun create(): GestureCallbacks = GestureCallbacks(
        onTap = onTap,
        onDoubleTap = onDoubleTap,
        onLongPressStart = onLongPressStart,
        onLongPressEnd = onLongPressEnd,
        onDragStart = onDragStart,
        onDrag = onDrag,
        onHorizontalDrag = onHorizontalDrag,
        onDragEnd = onDragEnd,
        ignoreDown = ignoreDown,
        swipe = GestureSwipeCallbacks(
            centerSwipeEnabled, onCenterSwipe, boostSwipeEnabled, onBoostSwipe,
        ),
    )
}
