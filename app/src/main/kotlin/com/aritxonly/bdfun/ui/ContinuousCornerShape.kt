package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.squircle.addSquircleRect

/**
 * 连续曲率圆角（squircle），与 HyperOS / Miuix 的轮廓一致，
 * 而不是普通圆弧圆角。
 */
class ContinuousCornerShape(private val radius: Dp) : Shape {
    // 调用点常常每次组合都新建一个；没有相等性判断时裁剪/轮廓缓存会一直失效。
    override fun equals(other: Any?): Boolean =
        other is ContinuousCornerShape && other.radius == radius

    override fun hashCode(): Int = radius.hashCode()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        if (size.width <= 0f || size.height <= 0f) {
            return Outline.Rectangle(
                androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height),
            )
        }
        val path = Path().apply {
            addSquircleRect(
                width = size.width,
                height = size.height,
                cornerRadius = with(density) { radius.toPx() },
            )
        }
        return Outline.Generic(path)
    }
}
