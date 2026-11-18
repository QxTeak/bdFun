package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

/** 连续手势之间共用单/双击窗口，单次手势状态不跨按下保留。 */
internal class PlayerTapStateHolder {
    var atMs = 0L
    var pending: Job? = null
}

internal class PlayerGestureSession(
    initial: Offset,
    private val width: () -> Float,
    private val touchSlop: () -> Float,
    private val leftSide: Boolean,
    private val verticalAllowed: Boolean,
    private val centerZone: Boolean,
    private val scope: CoroutineScope,
    private val taps: PlayerTapStateHolder,
    private val callbacks: () -> GestureCallbacks,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var press = callbacks().captureDanmaku(initial)
    private var handedOff = false
    private var dragged = false
    private var horizontal = false
    private var dragVertical = false
    private var centerSwipe = false
    private var centerSwipeDy = 0f
    private var totalDy = 0f
    private var totalDx = 0f
    private var previous = initial
    private var longPressed = false
    private val downTime = now()
    private lateinit var longPressJob: Job

    fun startLongPress(): Job {
        longPressJob = scope.launch {
            delay(380)
            if (!dragged) {
                press?.cancel()
                press = null
                longPressed = true
                taps.atMs = 0
                taps.pending?.cancel()
                callbacks().onLongPressStart()
            }
        }
        return longPressJob
    }

    fun released() {
        if (dragged || longPressed) return
        val held = now() - downTime
        if (held >= 300) return
        val time = now()
        if (time - taps.atMs < 260) {
            taps.atMs = 0
            taps.pending?.cancel()
            taps.pending = null
            press?.cancel()
            press = null
            callbacks().onDoubleTap()
        } else {
            taps.atMs = time
            taps.pending?.cancel()
            val captured = press
            handedOff = true
            var confirmed = false
            taps.pending = scope.launch {
                delay(260)
                if (taps.atMs != 0L) {
                    if (captured != null) { captured.confirm(); confirmed = true }
                    else callbacks().onTap()
                }
            }.also { job -> job.invokeOnCompletion { if (!confirmed) captured?.cancel() } }
        }
    }

    fun move(position: Offset, consume: () -> Unit) {
        totalDx += position.x - previous.x
        totalDy += position.y - previous.y
        previous = position
        if (longPressed) {
            // 长按加速中只处理水平滑动倍速。
            if (callbacks().swipe.boostSwipeEnabled) callbacks().swipe.onBoostSwipe(totalDx / width())
            consume()
            return
        }
        if (!dragged && (abs(totalDy) > touchSlop() || abs(totalDx) > touchSlop())) {
            press?.cancel()
            press = null
            dragged = true
            horizontal = abs(totalDx) > abs(totalDy)
            dragVertical = !horizontal && verticalAllowed
            centerSwipe = !horizontal && centerZone && callbacks().swipe.centerSwipeEnabled
            longPressJob.cancel()
            if (!centerSwipe) callbacks().onDragStart(leftSide, horizontal, verticalAllowed)
        }
        if (dragged) {
            if (centerSwipe) {
                centerSwipeDy = totalDy
                consume()
            } else if (horizontal) {
                callbacks().onHorizontalDrag(totalDx / width())
            } else if (dragVertical) {
                callbacks().onDrag(leftSide, totalDy)
            }
            if (!centerSwipe && (horizontal || dragVertical)) consume()
        }
    }

    fun finish(centerThreshold: Float) {
        if (!handedOff) { press?.cancel(); press = null }
        if (dragged && (horizontal || dragVertical)) callbacks().onDragEnd()
        if (centerSwipe && abs(centerSwipeDy) > centerThreshold) callbacks().swipe.onCenterSwipe()
        if (longPressed) callbacks().onLongPressEnd()
    }
}
