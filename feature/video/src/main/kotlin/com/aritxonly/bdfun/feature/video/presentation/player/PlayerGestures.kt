package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal data class GestureCallbacks(
    val onTap: () -> Unit,
    val onDoubleTap: () -> Unit,
    val onLongPressStart: () -> Unit,
    val onLongPressEnd: () -> Unit,
    val onDragStart: (Boolean, Boolean, Boolean) -> Unit,
    val onDrag: (Boolean, Float) -> Unit,
    val onHorizontalDrag: (Float) -> Unit,
    val onDragEnd: () -> Unit,
    val ignoreDown: (Offset, Float) -> Boolean,
    val swipe: GestureSwipeCallbacks,
)

internal data class GestureSwipeCallbacks(
    val centerSwipeEnabled: Boolean,
    val onCenterSwipe: () -> Unit,
    val boostSwipeEnabled: Boolean,
    val onBoostSwipe: (Float) -> Unit,
)

@Composable
internal fun Modifier.playerGestures(currentCallbacks: GestureCallbacks, verticalGesturesEnabled: () -> Boolean): Modifier {
    val setting by rememberUpdatedState(verticalGesturesEnabled)
    val taps = remember { PlayerTapStateHolder() }
    val scope = rememberCoroutineScope()
    // 保持 pointerInput(Unit) 的识别过程，事件触发时读取当前回调。
    val callbacks by rememberUpdatedState(currentCallbacks)
    return pointerInput(Unit) {
        awaitEachGesture {
            detectPlayerGesture(awaitFirstDown(requireUnconsumed = false), scope, taps,
                { setting() }) { callbacks }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.detectPlayerGesture(
    down: androidx.compose.ui.input.pointer.PointerInputChange,
    scope: kotlinx.coroutines.CoroutineScope,
    taps: PlayerTapStateHolder,
    verticalGesturesEnabled: () -> Boolean,
    callbacks: () -> GestureCallbacks,
) {
    if (callbacks().ignoreDown(down.position, size.height.toFloat())) {
        drainPlayerTouches()
        return
    }
    // 系统返回手势区域：24dp 内完全放行，避免抢系统侧滑。
    val systemEdge = 24.dp.toPx()
    if (down.position.x < systemEdge || down.position.x > size.width - systemEdge) {
        drainPlayerTouches()
        return
    }
    val side = when {
        down.position.x < size.width * 0.28f -> true
        down.position.x > size.width * 0.72f -> false
        else -> null
    }
    val smallPlayer = size.height < 320.dp.toPx()
    val verticalAllowed = verticalGesturesEnabled() && side != null &&
        down.position.y > (if (smallPlayer) 24.dp.toPx() else 80.dp.toPx()) &&
        down.position.y < size.height - (if (smallPlayer) 40.dp.toPx() else 100.dp.toPx())
    val gesture = PlayerGestureSession(
        down.position, { size.width.toFloat() }, { viewConfiguration.touchSlop },
        side ?: (down.position.x < size.width / 2f), verticalAllowed, side == null,
        scope, taps, callbacks,
    )
    val longPressJob = gesture.startLongPress()
    try {
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            if (!change.pressed) {
                gesture.released()
                break
            }
            gesture.move(change.position) { change.consume() }
        }
    } finally {
        longPressJob.cancel()
    }
    gesture.finish(70.dp.toPx())
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.drainPlayerTouches() {
    while (true) {
        val event = awaitPointerEvent()
        if (event.changes.none { it.pressed }) return
    }
}

/** 长按加速的触感反馈：交给系统默认的长按触感，不再自己写死时长/振幅。 */
internal fun buzzDevice(context: Context) {
    runCatching {
        @Suppress("DEPRECATION")
        val activity = context as? android.app.Activity
        activity?.window?.decorView
            ?.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
    }
}

internal fun currentBrightness(activity: Activity?): Float {
    val value = activity?.window?.attributes?.screenBrightness ?: -1f
    return if (value < 0f) 0.5f else value
}

internal fun applyBrightness(activity: Activity?, value: Float) {
    val window = activity?.window ?: return
    window.attributes = window.attributes.apply {
        screenBrightness = value.coerceIn(0.02f, 1f)
    }
}

internal fun currentVolumeRatio(audio: AudioManager): Float {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    return audio.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
}

internal fun setVolumeRatio(audio: AudioManager, ratio: Float) {
    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
    audio.setStreamVolume(
        AudioManager.STREAM_MUSIC,
        (ratio * max).toInt().coerceIn(0, max),
        0,
    )
}
