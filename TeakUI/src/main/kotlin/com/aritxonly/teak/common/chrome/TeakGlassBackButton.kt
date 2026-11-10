package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassRecipes

/**
 * 页面级返回键：progress = 0 时只有箭头（无背景），滚出顶部后逐渐露出玻璃底。
 *
 * 按钮组件为 Teak 新写；玻璃背景和 recipe 直接复用 HyperPiliPlus 的玻璃材质实现。
 * 来源记录见 docs/PROVENANCE.md。
 *
 * `revealed` 为 true 时与底栏、顶栏使用同一套 recipe / 同一块 backdrop；
 * [light] = true 用于视频 / 评论页（背后是 SurfaceView，采不到画面），固定白色箭头。
 *
 * 必须由采集层之外的覆盖层绘制，否则 backdrop 会与当前页面形成 RenderNode 自引用。
 */
@Composable
fun TeakGlassBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = TeakChromeDefaults.BackButtonSize,
    revealed: Boolean = true,
    light: Boolean = false,
) {
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    val glyph = if (light) Color.White else teakGlassGlyphColor()
    // 固定速率淡入/淡出（300ms），与页面滚动速度无关。
    val fallbackReveal = rememberManualProgress(if (revealed) 1f else 0f, 300f)
    val reveal = if (LocalProgressiveTopBarMaterialHosted.current) LocalTopBarButtonMaterialProgress.current else fallbackReveal

    Box(modifier = modifier.size(size)) {
        if (reveal > 0.01f) {
            val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
            if (classic) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = reveal }
                        .clip(CircleShape)
                        .background(top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHigh)
                        .clickable(interactionSource = null, indication = null, onClick = onClick),
                )
            } else {
                Box(modifier = Modifier.matchParentSize().graphicsLayer { alpha = reveal }) {
                    TeakGlassIconButton(
                        onClick = onClick,
                        size = size,
                        recipe = recipe,
                        backdrop = LocalTeakBackdrop.current,
                    ) {}
                }
            }
        }
        Canvas(modifier = Modifier.size(size * 0.50f).align(Alignment.Center)) {
            val w = this.size.width
            val h = this.size.height
            val chevron = Path().apply {
                moveTo(w * 0.66f, h * 0.12f)
                lineTo(w * 0.30f, h * 0.5f)
                lineTo(w * 0.66f, h * 0.88f)
            }
            val stroke = w * 0.10f
            if (!light) {
                // 没有背景时给一点点投影，保证压在白底上也看得清。
                drawPath(
                    path = chevron,
                    color = Color.Black.copy(alpha = 0.12f * (1f - reveal)),
                    style = Stroke(
                        width = stroke + 1.2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            drawPath(
                path = chevron,
                color = glyph,
                style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        // 透明热区：没有背景时也能点。
        if (reveal <= 0.01f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null, onClick = onClick),
            )
        }
    }
}
