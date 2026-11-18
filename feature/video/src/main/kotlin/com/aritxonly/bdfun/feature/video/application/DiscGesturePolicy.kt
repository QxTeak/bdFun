package com.aritxonly.bdfun.feature.video.application

import kotlin.math.abs

enum class DiscReleaseAction { Next, Previous, Exit, TogglePlayback, Open, None }
data class DiscDragOffset(val x: Float, val y: Float, val armed: Boolean)

/** 同样大小的对角滑动先判横向；短触使用原 Offset 计算的距离和严格边界。 */
fun discReleaseAction(x: Float, y: Float, distance: Float, longPress: Boolean,
    threshold: Float, touchSlop: Float): DiscReleaseAction = when {
    // 长按已经直接切换了暂停/继续，抬手不再触发其它动作。
    longPress -> DiscReleaseAction.None
    abs(x) >= threshold && abs(x) >= abs(y) -> if (x < 0f) DiscReleaseAction.Next else DiscReleaseAction.Previous
    y <= -threshold && abs(y) > abs(x) -> DiscReleaseAction.Exit
    y >= threshold && abs(y) > abs(x) -> DiscReleaseAction.TogglePlayback
    distance < touchSlop -> DiscReleaseAction.Open
    else -> DiscReleaseAction.None
}

fun discDragOffset(x: Float, y: Float, wasArmed: Boolean, threshold: Float, max: Float): DiscDragOffset {
    val armed = wasArmed || abs(x) >= threshold || abs(y) >= threshold
    if (!armed) return DiscDragOffset(0f, 0f, false)
    return if (abs(x) >= abs(y)) DiscDragOffset(x.coerceIn(-max, max), 0f, true)
    else DiscDragOffset(0f, y.coerceIn(-max, max), true)
}
