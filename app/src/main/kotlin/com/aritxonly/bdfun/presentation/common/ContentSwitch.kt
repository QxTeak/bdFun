package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 页内标签切换时的内容过渡：顶部的标签/头部留在原地，只有下面的内容滑进来。
 *
 * 和空间页翻页同一套「新内容从切换方向滑入」的观感，但这里是同一个列表换了数据，
 * 所以不搬 pager，而是让列表项统一读一份进度：进度只由 key 变化驱动，
 * 之后滚动时才组合出来的卡片读到的是 1，不会每张卡都重播一次入场。
 */
internal class ContentSwitchState {
    /** 1 = 稳定；0 = 刚切换、内容还没滑到位。 */
    var progress by mutableFloatStateOf(1f)
    /** 1 从左往右、-1 从右往左；0 只淡入不位移。 */
    var direction by mutableFloatStateOf(0f)

    fun restart(direction: Int) {
        this.direction = direction.toFloat()
        progress = 0f
    }
}

internal val LocalContentSwitch = compositionLocalOf<ContentSwitchState?> { null }

/**
 * [key] 变化时重播一次内容过渡。[direction] 由调用方按标签索引差给出（+1/-1）。
 */
@Composable
internal fun rememberContentSwitch(key: Any?, direction: Int): ContentSwitchState {
    val state = remember { ContentSwitchState() }
    var last by remember { mutableStateOf<Any?>(null) }
    LaunchedEffect(key) {
        if (last != null && key != last) {
            state.restart(direction)
            animate(
                initialValue = 0f,
                targetValue = 1f,
                animationSpec = tween(durationMillis = ContentSwitchDurationMs, easing = LinearOutSlowInEasing),
            ) { value, _ -> state.progress = value }
        }
        last = key
    }
    return state
}

/** 内容项这一层读取共享进度：透明度 + 水平位移，滑动距离固定，不随卡片宽度变化。 */
@Composable
internal fun Modifier.contentSwitchItem(offsetPx: Float = 26f): Modifier {
    val switch = LocalContentSwitch.current ?: return this
    return this.graphicsLayer {
        val progress = switch.progress
        alpha = progress
        translationX = (1f - progress) * offsetPx * switch.direction
    }
}

private const val ContentSwitchDurationMs = 320
