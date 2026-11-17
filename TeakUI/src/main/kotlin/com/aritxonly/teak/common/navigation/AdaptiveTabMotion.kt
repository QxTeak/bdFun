package com.aritxonly.teak.common.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal class AdaptiveTabMotion(val scroll: ScrollState) {
    var initialized = false
    private var automatic by mutableStateOf(false)
    private var indicator by mutableFloatStateOf(0f)

    fun indicator(position: Float, geometry: AdaptiveTabGeometry): Float =
        if (automatic) indicator else position * geometry.item - scroll.value

    suspend fun move(position: Float, geometry: AdaptiveTabGeometry, animated: Boolean) {
        val target = geometry.scroll(position)
        val targetIndicator = geometry.indicator(position)
        // 同一个时钟驱动两层；中间项之间切换时指示器起终点相同，不跟着文字晃动。
        val fromIndicator = indicator
        scroll.scroll {
            val fromScroll = scroll.value.toFloat()
            automatic = true
            try {
                if (animated) {
                    animate(0f, 1f, animationSpec = tween(420, easing = CubicBezierEasing(.22f, 1f, .36f, 1f))) { progress, _ ->
                        scrollBy(fromScroll + (target - fromScroll) * progress - scroll.value)
                        indicator = fromIndicator + (targetIndicator - fromIndicator) * progress
                    }
                } else {
                    scrollBy(target - scroll.value)
                    indicator = targetIndicator
                }
            } finally {
                automatic = false
            }
        }
    }

    fun recordManualPosition(position: Float, geometry: AdaptiveTabGeometry) {
        if (!automatic) indicator = position * geometry.item - scroll.value
    }
}
