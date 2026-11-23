/*
 * Adapted from HyperIsland (https://github.com/1812z/HyperIsland), GPL-3.0.
 * 参考它的 About 页动态渐变背景：多组缓慢漂移的柔和光斑 + 底部渐隐，
 * 滚动离开首屏时整层淡出。
 */
package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@Composable
internal fun SettingsAboutFloatingBackground(
    modifier: Modifier = Modifier,
    alpha: () -> Float,
    animationsEnabled: Boolean,
) {
    val animationTime = rememberAboutAnimationTime(animationsEnabled)
    val surface = MiuixTheme.colorScheme.background
    val dark = surface.luminance() < 0.5f
    // DstIn 渐隐只能作用在这一层，不能把页面其它内容一起擦掉。
    Canvas(modifier = modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
        drawRect(color = surface)
        val shown = alpha().coerceIn(0f, 1f)
        if (shown <= 0f) return@Canvas
        // 时间只在绘制阶段读：动画每帧只重画这一层，不重组整个页面。
        val time = animationTime.floatValue
        val colors = animatedAboutColors(time, dark)
        drawAboutGradientField(
            animationTime = time,
            colors = colors,
            fieldSize = size,
            strength = shown,
        )
        // 底部柔和收进页面底色，避免光斑在列表末尾突然被切断。
        drawRect(
            brush = Brush.verticalGradient(
                colorStops = arrayOf(
                    0f to Color.White,
                    0.68f to Color.White,
                    1f to Color.Transparent,
                ),
            ),
            blendMode = BlendMode.DstIn,
        )
        drawRect(color = surface.copy(alpha = (1f - 0.72f) * shown))
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAboutGradientField(
    animationTime: Float,
    colors: List<Color>,
    fieldSize: Size,
    strength: Float,
) {
    val radius = fieldSize.maxDimension * 0.62f
    val motion = animationTime * ABOUT_BACKGROUND_SPEED
    // 底层铺一层斜向渐变，让光斑之间的空白也有颜色。
    drawRect(
        brush = Brush.linearGradient(
            colors = colors.map { it.copy(alpha = 0.58f * strength) },
            start = Offset(-fieldSize.width * 0.2f, -fieldSize.height * 0.2f),
            end = Offset(fieldSize.width * 1.2f, fieldSize.height),
        ),
    )
    val centers = listOf(
        Offset(
            x = fieldSize.width * (0.18f + 0.10f * sin(motion)),
            y = fieldSize.height * (0.20f + 0.08f * cos(motion * 0.8f)),
        ),
        Offset(
            x = fieldSize.width * (0.82f + 0.10f * cos(motion * 0.9f)),
            y = fieldSize.height * (0.78f + 0.10f * sin(motion * 0.7f)),
        ),
        Offset(
            x = fieldSize.width * (0.22f + 0.12f * cos(motion * 0.65f)),
            y = fieldSize.height * (0.80f + 0.08f * sin(motion * 0.85f)),
        ),
        Offset(
            x = fieldSize.width * (0.80f + 0.12f * sin(motion * 0.72f)),
            y = fieldSize.height * (0.20f + 0.08f * cos(motion * 0.62f)),
        ),
    )
    centers.forEachIndexed { index, center ->
        val color = colors[index % colors.size]
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    color.copy(alpha = 0.88f * strength),
                    color.copy(alpha = 0f),
                ),
                center = center,
                radius = radius,
            ),
            center = center,
            radius = radius,
            blendMode = BlendMode.SrcOver,
        )
    }
}

/** 与 HyperIsland 一样按帧累积时间；关闭动画时停在 0，不产生无谓的重绘。 */
@Composable
private fun rememberAboutAnimationTime(animationsEnabled: Boolean): androidx.compose.runtime.MutableFloatState {
    val time = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) return@LaunchedEffect
        var previous = 0L
        while (true) {
            withFrameNanos { frameTime ->
                if (previous != 0L) {
                    time.floatValue += (frameTime - previous) / 1_000_000_000f
                }
                previous = frameTime
            }
        }
    }
    return time
}

private fun animatedAboutColors(time: Float, dark: Boolean): List<Color> {
    val palettes = if (dark) DarkPalettes else LightPalettes
    val segmentValue = time / ABOUT_COLOR_CROSSFADE_SECONDS
    val segment = floor(segmentValue).toInt() % palettes.size
    val raw = segmentValue - floor(segmentValue)
    // smoothstep，避免两套配色在交界处硬切。
    val progress = raw * raw * (3f - 2f * raw)
    val start = palettes[segment]
    val end = palettes[(segment + 1) % palettes.size]
    return start.indices.map { index ->
        androidx.compose.ui.graphics.lerp(start[index], end[index], progress)
    }
}

/** 颜色更饱和一点，铺在页面底色上才看得见；与 HyperIsland 的 strengthen 同思路。 */
private fun strengthen(color: Color): Color {
    val average = (color.red + color.green + color.blue) / 3f
    val saturation = 1.18f
    return Color(
        red = (average + (color.red - average) * saturation).coerceIn(0f, 1f),
        green = (average + (color.green - average) * saturation).coerceIn(0f, 1f),
        blue = (average + (color.blue - average) * saturation).coerceIn(0f, 1f),
        alpha = color.alpha,
    )
}

private const val ABOUT_BACKGROUND_SPEED = 0.12f
private const val ABOUT_COLOR_CROSSFADE_SECONDS = 12f

private val LightPalettes = listOf(
    listOf(Color(1f, 0.90f, 0.94f), Color(1f, 0.84f, 0.89f), Color(0.97f, 0.73f, 0.82f), Color(0.64f, 0.65f, 0.98f)).map(::strengthen),
    listOf(Color(0.58f, 0.74f, 1f), Color(1f, 0.90f, 0.93f), Color(0.74f, 0.76f, 1f), Color(0.97f, 0.77f, 0.84f)).map(::strengthen),
    listOf(Color(0.98f, 0.86f, 0.90f), Color(0.60f, 0.73f, 0.98f), Color(0.92f, 0.93f, 1f), Color(0.56f, 0.69f, 1f)).map(::strengthen),
)

private val DarkPalettes = listOf(
    listOf(Color(0.20f, 0.06f, 0.88f, 0.40f), Color(0.30f, 0.14f, 0.55f, 0.50f), Color(0f, 0.64f, 0.96f, 0.50f), Color(0.11f, 0.16f, 0.83f, 0.40f)),
    listOf(Color(0.07f, 0.15f, 0.79f, 0.50f), Color(0.62f, 0.21f, 0.67f, 0.50f), Color(0.06f, 0.25f, 0.84f, 0.50f), Color(0f, 0.20f, 0.78f, 0.50f)),
    listOf(Color(0.58f, 0.30f, 0.74f, 0.40f), Color(0.27f, 0.18f, 0.60f, 0.50f), Color(0.66f, 0.26f, 0.62f, 0.50f), Color(0.12f, 0.16f, 0.70f, 0.60f)),
)
