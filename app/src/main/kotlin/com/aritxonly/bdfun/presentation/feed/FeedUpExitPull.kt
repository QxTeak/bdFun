package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.dp

/** 下拉多远时一镜到底退场动画播到最后一帧。 */
internal val FeedUpExitPullDistance = 200.dp

/**
 * 快速下拉的判定速度（dp/s）。
 *
 * 距离不够但手指甩得够快时直接播完退场，不必真的拖满 200dp；
 * 慢慢拉则按下拉距离逐帧倒放，松手时没过线再弹回 UP 页。
 */
internal val FeedUpExitFlingVelocity = 1000.dp

/** 松手时退场动画已播到这个比例（progress 越低表示越接近动态页），直接完成退出。 */
internal const val FeedUpExitCompleteProgress = 0.35f

/** 下拉位移 → 一镜到底进度：1 = 完整 UP 页，0 = 完全回到动态页。 */
internal fun feedUpExitProgress(dragPx: Float, distancePx: Float): Float {
    if (distancePx <= 0f) return 1f
    return (1f - dragPx / distancePx).coerceIn(0f, 1f)
}

/** 松手后是否完成退出：拉过线或快速往下甩。 */
internal fun shouldCompleteFeedUpExit(
    progress: Float,
    velocityPxPerSec: Float,
    flingVelocityPxPerSec: Float,
): Boolean = progress <= FeedUpExitCompleteProgress || velocityPxPerSec >= flingVelocityPxPerSec
