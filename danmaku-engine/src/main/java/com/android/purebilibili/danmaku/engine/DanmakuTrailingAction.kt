package com.android.purebilibili.danmaku.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF

/** 随弹幕绘制的尾部按钮，同一播放器内共享位图和画笔。 */
class DanmakuTrailingAction(
    private val icon: Bitmap,
    color: Int,
    activeColor: Int,
    private val isActive: (Long) -> Boolean,
    internal val onClick: (Long) -> Unit,
) {
    private val normal = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    private val active = PorterDuffColorFilter(activeColor, PorterDuff.Mode.SRC_IN)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val bounds = RectF()

    internal fun draw(canvas: Canvas, id: Long, right: Float, centerY: Float, lineHeight: Float) {
        val size = lineHeight * 0.72f
        val left = right - (advance(lineHeight) + size) / 2f
        val top = centerY - size / 2f
        bounds.set(left, top, left + size, top + size)
        paint.colorFilter = if (isActive(id)) active else normal
        canvas.drawBitmap(icon, null, bounds, paint)
    }

    internal companion object {
        fun advance(lineHeight: Float): Float = lineHeight * 1.25f

        fun contains(rect: RectF, x: Float, y: Float): Boolean =
            rect.contains(x, y) && x >= rect.right - advance(rect.height())
    }
}
