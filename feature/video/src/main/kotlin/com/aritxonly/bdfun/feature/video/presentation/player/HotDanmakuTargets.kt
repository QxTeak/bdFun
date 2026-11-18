package com.aritxonly.bdfun.ui

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

internal class HotDanmakuTargets {
    var coordinates: LayoutCoordinates? = null
    val bounds = mutableListOf<Pair<Long, RectF>>()
    fun capture(window: Offset, like: (Long) -> Unit): PlayerDanmakuPress? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val point = layout.windowToLocal(window)
        val id = bounds.firstOrNull { it.second.contains(point.x, point.y) }?.first ?: return null
        return PlayerDanmakuPress({ like(id) }, {})
    }
}

private val thumbPath = Path().apply {
    moveTo(7f, 10f); lineTo(12f, 3f); lineTo(14f, 3f); lineTo(14f, 9f)
    lineTo(20f, 9f); lineTo(20f, 12f); lineTo(17f, 21f); lineTo(7f, 21f); close()
    addRect(2f, 10f, 5f, 21f, Path.Direction.CW)
}
private val thumbPaint = Paint(Paint.ANTI_ALIAS_FLAG)

/** 仅新增图标，正文、计数的倾斜字体和描边沿用原绘制。 */
internal fun drawHotLikeIcon(canvas: Canvas, x: Float, y: Float, size: Float, liked: Boolean) {
    canvas.save()
    canvas.translate(x, y)
    canvas.scale(size / 24f, size / 24f)
    thumbPaint.color = if (liked) 0xFFFF6699.toInt() else android.graphics.Color.WHITE
    thumbPaint.style = if (liked) Paint.Style.FILL else Paint.Style.STROKE
    thumbPaint.strokeWidth = 1.7f
    canvas.drawPath(thumbPath, thumbPaint)
    canvas.restore()
}
