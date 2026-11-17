package com.aritxonly.teak.common.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
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

    fun value(external: Float): Float = if (owned) localPosition else external

    fun begin(external: Float) {
        val start = value(external)
        generation++
        animation?.cancel()
        localPosition = start
        owned = true
    }

    fun drag(delta: Float, item: Float, count: Int) {
        localPosition = draggedTabPosition(localPosition, delta, item, count)
    }

    fun end(velocity: Float, item: Float, count: Int): Int {
        return settledTabIndex(localPosition, velocity, item, count)
    }

    fun select(
        target: Int, external: () -> Float, scope: CoroutineScope,
        enabled: Boolean, onSelect: (Int) -> Unit,
    ) {
        val start = value(external())
        val ticket = ++generation
        animation?.cancel()
        localPosition = start
        owned = true
        onSelect(target)
        animation = scope.launch {
            try {
                if (enabled) animate(start, target.toFloat(), animationSpec = tween(280,
                    easing = CubicBezierEasing(.22f, 1f, .36f, 1f))) { value, _ -> localPosition = value }
                else localPosition = target.toFloat()
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
