package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Rect

/**
 * 头像滑出视口的方向。环下一次从屏幕外出发时必须用它决定从哪一侧滑进来，
 * 不能到了播放动画时再拿旧坐标猜：头像被回收后，最后测到的位置可能已经不在那一侧。
 */
internal enum class FeedRingSide { Left, Right }

/**
 * 头像的「可见侧」：完全在视口内返回 null；只露出一部分时返回探出去的那一侧；
 * 完全在视口外按中心点归到最近的一侧。
 */
internal fun feedRingExitSide(rect: Rect, viewportWidth: Float): FeedRingSide? {
    if (viewportWidth <= 0f) return null
    return when {
        rect.right <= 0f -> FeedRingSide.Left
        rect.left >= viewportWidth -> FeedRingSide.Right
        rect.left < 0f -> FeedRingSide.Left
        rect.right > viewportWidth -> FeedRingSide.Right
        else -> null
    }
}

/**
 * 头像已经被回收时用的起点圆：圆心整个落在视口外，纵向对齐目标那一行。
 *
 * 起点圆只用来算「线从哪伸出来」，本身不画环；所以它必须完全在屏幕外，
 * 线才会真的从边缘滑进来，而不是从屏幕内的某个坐标凭空长出来。
 */
internal fun feedRingOffscreenStart(
    side: FeedRingSide,
    avatarSize: Float,
    centerY: Float,
    viewportWidth: Float,
): Rect {
    val size = avatarSize.coerceAtLeast(1f)
    val half = size / 2f
    val centerX = when (side) {
        FeedRingSide.Left -> -half
        FeedRingSide.Right -> viewportWidth + half
    }
    return Rect(centerX - half, centerY - half, centerX + half, centerY + half)
}

/** 起点圆是否完整落在视口外（此时源头像的环不该画，只剩一条线）。 */
internal fun feedRingIsOffscreen(rect: Rect, viewportWidth: Float): Boolean =
    rect.right <= 0f || rect.left >= viewportWidth

/** 没有任何方向记录时的兜底：从离它最近的那一侧进来。 */
internal fun feedRingNearestSide(rect: Rect, viewportWidth: Float): FeedRingSide =
    if (rect.left + rect.width / 2f < viewportWidth / 2f) FeedRingSide.Left else FeedRingSide.Right
