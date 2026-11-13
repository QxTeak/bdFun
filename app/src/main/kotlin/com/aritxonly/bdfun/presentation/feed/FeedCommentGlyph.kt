package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

@Composable
internal fun CommentGlyph(tint: Color, size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = w * 0.085f
        val bodyTop = h * 0.10f
        val bodyHeight = h * 0.62f
        val bodyLeft = w * 0.09f
        val bodyWidth = w * 0.82f
        drawRoundRect(
            color = tint,
            topLeft = Offset(bodyLeft, bodyTop),
            size = Size(bodyWidth, bodyHeight),
            cornerRadius = CornerRadius(w * 0.20f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke),
        )
        // 左下角的尾巴
        val tail = androidx.compose.ui.graphics.Path().apply {
            moveTo(w * 0.27f, bodyTop + bodyHeight - stroke * 0.4f)
            lineTo(w * 0.24f, h * 0.90f)
            lineTo(w * 0.47f, bodyTop + bodyHeight - stroke * 0.4f)
        }
        drawPath(
            path = tail,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round,
            ),
        )
    }
}
