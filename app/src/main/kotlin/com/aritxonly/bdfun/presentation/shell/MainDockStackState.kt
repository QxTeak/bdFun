package com.aritxonly.bdfun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.util.VelocityTracker
import com.aritxonly.bdfun.ui.BackgroundPlayback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.max

/**
 * 搜索卡 / 唱片卡的堆叠切换状态。
 *
 * 自动切换（后台播放开始/结束）和手指切换共用同一组 [Animatable] 与代数守卫；
 * 任何一方被另一方打断时，旧协程看到代数变化就不再提交，避免迟到的 finally 改新状态。
 * 手势判定、提交和渲染都只读这里的单一真相，便于拆成小函数。
 */
internal class DockStackStateHolder(
    private val slotPx: Float,
    private val scope: CoroutineScope,
    private val horizontalTriggerPx: Float,
) {
    var showDisc by mutableStateOf(false)
    var gesture by mutableStateOf(DockGesture.None)
    var switchDrag by mutableFloatStateOf(0f)
    var dismissDrag by mutableFloatStateOf(0f)
    var horizontalDrag by mutableFloatStateOf(0f)
    var animating by mutableStateOf(false)
    /** 正在退出的那张卡；动画结束后 [showDisc] 才会翻面。 */
    var fromDisc by mutableStateOf(false)
    private var generation = 0
    private val switchAnim = Animatable(0f)
    private val dismissAnim = Animatable(0f)
    private val horizontalAnim = Animatable(0f)

    val currentIsDisc: Boolean get() = if (animating) fromDisc else showDisc
    val incomingIsDisc: Boolean get() = !currentIsDisc
    val switch: Float get() = if (animating) switchAnim.value else switchDrag
    val dismiss: Float get() = if (animating) dismissAnim.value else dismissDrag
    val horizontal: Float get() = if (animating) horizontalAnim.value else horizontalDrag
    val transition: Float get() = max(switch, dismiss)

    /** 后台播放开始/结束时自动走一次堆叠动画；手势期间等待，不抢当前手势。 */
    suspend fun syncAvailability(available: Boolean) {
        while (available != showDisc) {
            if (animating || gesture != DockGesture.None) {
                delay(16)
                continue
            }
            val gen = ++generation
            animating = true
            fromDisc = showDisc
            switchDrag = 0f
            try {
                switchAnim.snapTo(0f)
                switchAnim.animateTo(1f, DockSpring)
                if (gen == generation) showDisc = available
            } finally {
                if (gen == generation) {
                    switchDrag = 0f
                    switchAnim.snapTo(0f)
                    animating = false
                }
            }
        }
    }

    fun beginGesture() {
        if (animating) {
            generation++
            animating = false
            switchDrag = 0f
            scope.launch { switchAnim.stop() }
        }
        gesture = DockGesture.None
        switchDrag = 0f
        dismissDrag = 0f
        horizontalDrag = 0f
    }

    fun updateDrag(offset: Offset, discAvailable: Boolean) {
        if (gesture == DockGesture.None) {
            val horizontalAxis = abs(offset.x) > abs(offset.y)
            gesture = when {
                horizontalAxis && showDisc -> DockGesture.Horizontal
                !horizontalAxis && offset.y > 0f && discAvailable -> DockGesture.Switch
                !horizontalAxis && offset.y < 0f && showDisc -> DockGesture.Dismiss
                else -> DockGesture.None
            }
        }
        when (gesture) {
            DockGesture.Switch -> switchDrag = (offset.y / slotPx).coerceIn(0f, 0.6f)
            DockGesture.Dismiss -> dismissDrag = (-offset.y / slotPx).coerceIn(0f, 1f)
            DockGesture.Horizontal -> if (showDisc) {
                horizontalDrag = (offset.x / slotPx).coerceIn(-0.6f, 0.6f)
            }
            DockGesture.None -> Unit
        }
    }

    fun finishGesture(velocity: androidx.compose.ui.unit.Velocity) {
        when (gesture) {
            DockGesture.Switch -> commitSwitch(velocity.y)
            DockGesture.Dismiss -> commitDismiss(velocity.y)
            DockGesture.Horizontal -> commitHorizontal(velocity.x)
            DockGesture.None -> Unit
        }
        gesture = DockGesture.None
    }

    private fun commitSwitch(velocityY: Float) {
        val settled = switchDrag
        val commit = settled >= COMMIT_DISTANCE || velocityY >= VERTICAL_VELOCITY
        val gen = ++generation
        val outgoingIsDisc = showDisc
        animating = true
        fromDisc = outgoingIsDisc
        scope.launch {
            switchAnim.snapTo(settled)
            try {
                switchAnim.animateTo(
                    targetValue = if (commit) 1f else 0f,
                    animationSpec = DockSpring,
                    initialVelocity = velocityY / slotPx,
                )
                if (gen == generation && commit) showDisc = !outgoingIsDisc
            } finally {
                if (gen == generation) {
                    switchDrag = 0f
                    switchAnim.snapTo(0f)
                    animating = false
                }
            }
        }
    }

    private fun commitDismiss(velocityY: Float) {
        val settled = dismissDrag
        val commit = settled >= COMMIT_DISTANCE || velocityY <= -VERTICAL_VELOCITY
        val gen = ++generation
        val outgoingIsDisc = showDisc
        animating = true
        fromDisc = outgoingIsDisc
        scope.launch {
            dismissAnim.snapTo(settled)
            try {
                dismissAnim.animateTo(
                    targetValue = if (commit) 1f else 0f,
                    animationSpec = DockSpring,
                    initialVelocity = -velocityY / slotPx,
                )
                if (gen == generation && commit) {
                    BackgroundPlayback.exit()
                    showDisc = false
                }
            } finally {
                if (gen == generation) {
                    dismissDrag = 0f
                    dismissAnim.snapTo(0f)
                    animating = false
                }
            }
        }
    }

    private fun commitHorizontal(velocityX: Float) {
        val dx = horizontalDrag * slotPx
        val commit = abs(velocityX) >= HORIZONTAL_VELOCITY || abs(dx) >= horizontalTriggerPx
        if (commit) {
            if (dx < 0f) BackgroundPlayback.next() else BackgroundPlayback.previous()
        }
        val gen = ++generation
        animating = true
        fromDisc = showDisc
        scope.launch {
            horizontalAnim.snapTo(horizontalDrag)
            horizontalAnim.animateTo(0f, DockSpring)
            if (gen == generation) {
                horizontalDrag = 0f
                horizontalAnim.snapTo(0f)
                animating = false
            }
        }
    }
}

internal enum class DockGesture { None, Switch, Dismiss, Horizontal }

private const val COMMIT_DISTANCE = 0.3f
private const val VERTICAL_VELOCITY = 2000f
private const val HORIZONTAL_VELOCITY = 1200f

private val DockSpring = spring<Float>(
    dampingRatio = 0.9f,
    stiffness = (2f * Math.PI.toFloat() / 0.35f) * (2f * Math.PI.toFloat() / 0.35f),
)

/** 统一手势入口：方向判定、拖动进度、松手提交都交给 [DockStackStateHolder]。 */
internal suspend fun PointerInputScope.dockStackGestures(
    state: DockStackStateHolder,
    discAvailable: Boolean,
    slotPx: Float,
    tapSlop: Float,
    onTapSearch: () -> Unit,
    onTapDisc: () -> Unit,
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        // 从一开始就吃掉事件，子控件永远不会自己触发点击。
        down.consume()
        state.beginGesture()
        val tracker = VelocityTracker()
        tracker.addPosition(down.uptimeMillis, down.position)
        var moved = false
        var path = 0f
        while (true) {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            tracker.addPosition(change.uptimeMillis, change.position)
            val offset = change.position - down.position
            path += (change.position - change.previousPosition).getDistance()
            if (!moved && path > tapSlop) moved = true
            change.consume()
            if (moved) state.updateDrag(offset, discAvailable)
            if (!change.pressed) {
                if (!moved) {
                    if (state.showDisc) onTapDisc() else onTapSearch()
                } else {
                    state.finishGesture(tracker.calculateVelocity())
                }
                break
            }
        }
    }
}
