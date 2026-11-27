package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.abs

/** 旧页卡片滑出 / 新页卡片滑入的时长。两段加起来和空间页翻页一个量级。 */
private const val FeedUpExitMs = 150
private const val FeedUpEnterMs = 200

/** 拖动时卡片最多跟手多少（占页宽比例）。 */
private const val FeedUpDragLimit = 0.34f

/** 跟手阻尼：手指走 1px 卡片只走这么多，避免拖出大片空白。 */
private const val FeedUpDragResistance = 0.55f

/** 动画结束后允许的浮点残差。 */
private const val FeedUpOffsetEpsilon = 0.5f

/** 一次 UP 换页时两段卡片位移：旧页滑到 [exitOffset]，新页从 [enterOffset] 滑回 0。 */
internal data class FeedUpSlideOffsets(val exitOffset: Float, val enterOffset: Float)

internal fun feedUpSlideOffsets(direction: Int, width: Float): FeedUpSlideOffsets {
    val dir = direction.coerceIn(-1, 1)
    return FeedUpSlideOffsets(exitOffset = -dir * width, enterOffset = dir * width)
}

/**
 * UP 之间切换的左右推动。
 *
 * 只移动**动态卡片**：头像行（UP 栏）是选页控件，必须留在原地，不能跟着页面滑走。
 * 先让旧页卡片整排滑出，换完数据再让新页卡片从另一侧滑入；两段都是整页宽度，
 * 所以观感是左右推动，而不是之前那种 26px 淡入。
 *
 * 不抓整屏快照、不在整块内容上挂图层：那会带来整屏闪帧，也会把 UP 栏一起搬走。
 */
@Stable
internal class FeedUpPageTransition {
    /** 所有动态卡片的横向位移；拖动和两段动画都只改这一个值。 */
    var cardOffset by mutableFloatStateOf(0f)
        private set
    var busy by mutableStateOf(false)
        private set
    var widthPx = 0f

    /** 手指按下开始拖：确保从静止位置起步（上一次回弹没走完也不会叠加）。 */
    fun beginDrag() {
        if (busy) return
        cardOffset = 0f
    }

    fun dragBy(delta: Float) {
        if (busy) return
        val limit = (widthPx * FeedUpDragLimit).coerceAtLeast(1f)
        cardOffset = (cardOffset + delta * FeedUpDragResistance).coerceIn(-limit, limit)
    }

    /**
     * 松手结算。[direction] 为 +1 时下一个 UP（旧页往左出、新页从右进），-1 反之。
     * [commit] 为 null 表示没过阈值：只回弹，不换页。
     *
     * 返回 false 表示上一次推动还没播完，调用方应当直接换页而不是把这次操作吞掉。
     */
    suspend fun settle(direction: Int, commit: (suspend () -> Unit)?): Boolean {
        if (busy) return false
        val width = widthPx
        if (commit == null) {
            slideBack()
            return true
        }
        // 还没量到页宽就退化成直接换页，不能把这次切换吞掉。
        if (width <= 0f) {
            commit()
            return true
        }
        busy = true
        val dir = direction.coerceIn(-1, 1)
        val offsets = feedUpSlideOffsets(dir, width)
        try {
            // 第一段：旧页卡片滑出。此间数据还没换，头像行保持不动。
            animate(
                initialValue = cardOffset,
                targetValue = offsets.exitOffset,
                animationSpec = tween(FeedUpExitMs, easing = FastOutSlowInEasing),
            ) { value, _ -> cardOffset = value }
            // 换页；新页卡片先放到另一侧屏幕外，第二段再滑回 0。
            commit()
            cardOffset = offsets.enterOffset
            animate(
                initialValue = offsets.enterOffset,
                targetValue = 0f,
                animationSpec = tween(FeedUpEnterMs, easing = FastOutSlowInEasing),
            ) { value, _ -> cardOffset = value }
        } finally {
            cardOffset = 0f
            busy = false
        }
        return true
    }

    /** 没过阈值 / 拖到一半取消：用同一段缓动退回原位。 */
    private suspend fun slideBack() {
        val start = cardOffset
        if (abs(start) < FeedUpOffsetEpsilon) {
            cardOffset = 0f
            return
        }
        animate(
            initialValue = start,
            targetValue = 0f,
            animationSpec = tween(180, easing = FastOutSlowInEasing),
        ) { value, _ -> cardOffset = value }
        cardOffset = 0f
    }
}

/** 只让动态卡片参与位移；头像行不调用这个 Modifier，所以永远不动。 */
internal fun Modifier.feedUpCardOffset(transition: FeedUpPageTransition): Modifier =
    this.graphicsLayer { translationX = transition.cardOffset }

/**
 * 页面内容的外壳：只负责裁剪和提供页宽，不再套整屏图层/快照。
 * 卡片滑到边缘外会被裁掉，不会画到顶栏或底栏上。
 */
@Composable
internal fun FeedUpPageTransitionHost(
    transition: FeedUpPageTransition,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { transition.widthPx = it.width.toFloat() },
    ) {
        content()
    }
}
