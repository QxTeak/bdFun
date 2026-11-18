package com.aritxonly.teak.common.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.abs

/** 手势期间由控件拥有连续位置，页面追上后再交还，避免两套动画互相拉扯。 */
internal class AdaptiveTabMotion {
    private var owned by mutableStateOf(false)
    private var localPosition by mutableFloatStateOf(0f)
    private var animation: Job? = null
    private var generation = 0
    var velocity by mutableFloatStateOf(0f)
        private set

    fun value(external: Float): Float = if (owned) localPosition else external

    fun begin(external: Float) {
        val start = value(external)
        generation++
        animation?.cancel()
        localPosition = start
        velocity = 0f
        owned = true
    }

    fun drag(delta: Float, item: Float, count: Int) {
        localPosition = draggedTabPosition(localPosition, delta, item, count)
    }

    fun end(velocity: Float, item: Float, count: Int): Int {
        // 限制释放能量，快划仍只多推进一项，不能让弹簧冲过数个频道。
        this.velocity = (-velocity / item.coerceAtLeast(1f)).coerceIn(-8f, 8f)
        return settledTabIndex(localPosition, velocity, item, count)
    }

    fun select(
        target: Int, external: () -> Float, scope: CoroutineScope,
        enabled: Boolean, onSelect: (Int) -> Unit,
    ) {
        val start = value(external())
        val initialVelocity = velocity
        val ticket = ++generation
        animation?.cancel()
        localPosition = start
        owned = true
        onSelect(target)
        animation = scope.launch {
            try {
                if (enabled) animate(start, target.toFloat(), initialVelocity = initialVelocity,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow, visibilityThreshold = .001f)) { value, speed ->
                    localPosition = value
                    velocity = speed
                } else localPosition = target.toFloat()
                velocity = 0f
                // 页面动画可以略慢；追上前保持吸附位置，不倒退一帧。
                withTimeoutOrNull(1500) {
                    snapshotFlow { external() }.first { abs(it - target) < .001f }
                }
            } finally {
                if (generation == ticket) owned = false
            }
        }
    }
}
