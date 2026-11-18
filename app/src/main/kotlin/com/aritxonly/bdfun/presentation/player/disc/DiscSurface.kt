package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.teakGlassControlShadow

/** 动画层每侧预留的阴影空间；低于这个值时 dropShadow 的 RenderNode 会被方形边界裁掉。 */
internal val DiscShadowRoom = 40.dp

@Composable
internal fun DiscSurface(runtime: DiscRuntime, visual: DiscVisual, modifier: Modifier, outerSize: Dp? = null, shadowEnabled: Boolean = true) {
    Box(modifier = modifier.size(outerSize ?: (runtime.size + 16.dp)), contentAlignment = Alignment.Center) {
        if (visual.breathEnabled) DiscBreathGlow(visual)
        // 拖拽/退出时位移和淡出必须放在放大的图层上：阴影画在 52dp 唱片节点上可以溢出节点，
        // 但一旦同一节点带 graphicsLayer，RenderNode 的方形边界会把阴影裁成方块。
        Box(
            modifier = Modifier
                .requiredSize(if (shadowEnabled) runtime.size + DiscShadowRoom * 2 else runtime.size)
                .graphicsLayer {
                    translationX = visual.dragX
                    translationY = visual.dragY - visual.exitProgress * 26.dp.toPx()
                    alpha = 1f - visual.exitProgress
                    scaleX = if (runtime.ui.pressed) 0.96f else 1f
                    scaleY = if (runtime.ui.pressed) 0.96f else 1f
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(runtime.size)
                    .then(runtime.oneTake.modifier)
                    .then(if (shadowEnabled) Modifier.teakGlassControlShadow(CircleShape) else Modifier)
                    .discPointerInput(runtime),
                contentAlignment = Alignment.Center,
            ) {
                DiscRecord(runtime, visual)
            }
        }
    }
}

@Composable
private fun DiscBreathGlow(visual: DiscVisual) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - visual.exitProgress },
    ) {
        // 光圈比唱片外壳更大，允许画到 70dp 盒子之外，浅色背景下也能看出光晕在呼吸。
        val glowRadius = this.size.minDimension * (0.58f + 0.10f * visual.breath01)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    visual.breathTint.copy(alpha = 0.30f * visual.breath01),
                    visual.breathTint.copy(alpha = 0.72f * (0.30f + 0.70f * visual.breath01)),
                    visual.breathTint.copy(alpha = 0.20f * visual.breath01),
                    Color.Transparent,
                ),
                center = center,
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = center,
        )
    }
}
