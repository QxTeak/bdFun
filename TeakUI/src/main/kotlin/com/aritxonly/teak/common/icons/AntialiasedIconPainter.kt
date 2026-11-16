package com.aritxonly.teak.common.icons

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.VectorPainter

/** 小尺寸细线先以双倍像素缓存再缩小，避免旋转/非整数密度下直接放大低分辨率缓存。 */
@Composable
fun rememberAntialiasedIconPainter(source: Painter): Painter = remember(source) {
    if (source is VectorPainter) AntialiasedIconPainter(source) else source
}

private class AntialiasedIconPainter(private val source: Painter) : Painter() {
    private var alpha = 1f
    private var colorFilter: ColorFilter? = null
    override val intrinsicSize: Size get() = source.intrinsicSize

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    override fun DrawScope.onDraw() {
        val pixels = Size(size.width * 2f, size.height * 2f)
        scale(0.5f, pivot = Offset.Zero) {
            with(source) { draw(pixels, alpha, colorFilter) }
        }
    }
}
