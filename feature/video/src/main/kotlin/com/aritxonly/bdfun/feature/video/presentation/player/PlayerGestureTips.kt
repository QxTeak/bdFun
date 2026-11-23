package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule

@Composable
internal fun GestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LevelGlyph(brightness = brightness, value = value)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(Capsule())
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}

/**
 * 保留原来的 Lucide 喇叭/太阳图标作为底座，只在其外围叠加动态元素：
 * 音量按大小补 1/2/3 根弧线；亮度按高低改变外圈光线长度。
 */
@Composable
private fun LevelGlyph(brightness: Boolean, value: Float) {
    Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
        AppGlyph(
            if (brightness) LucideIcons.Sun else LucideIcons.Volume1,
            Color.White,
            size = 20.dp,
        )
        androidx.compose.foundation.Canvas(Modifier.size(24.dp)) {
            val w = size.width
            val h = size.height
            val stroke = 1.3.dp.toPx()
            if (brightness) {
                val cx = w * 0.5f
                val cy = h * 0.5f
                val inner = w * 0.37f
                val outer = inner + w * 0.13f * value.coerceIn(0f, 1f)
                for (index in 0 until 8) {
                    val angle = Math.toRadians((index * 45).toDouble())
                    val dx = kotlin.math.cos(angle).toFloat()
                    val dy = kotlin.math.sin(angle).toFloat()
                    drawLine(
                        color = Color.White.copy(alpha = 0.9f),
                        start = androidx.compose.ui.geometry.Offset(cx + dx * inner, cy + dy * inner),
                        end = androidx.compose.ui.geometry.Offset(cx + dx * outer, cy + dy * outer),
                        strokeWidth = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            } else {
                val arcs = when {
                    value < 0.34f -> 1
                    value < 0.67f -> 2
                    else -> 3
                }
                // 原图标自带第一根弧线，这里只补第 2/3 根。
                for (index in 1 until arcs) {
                    val radius = w * (0.29f + 0.09f * (index - 1))
                    drawArc(
                        color = Color.White,
                        startAngle = -52f,
                        sweepAngle = 104f,
                        useCenter = false,
                        topLeft = androidx.compose.ui.geometry.Offset(w * 0.50f - radius, h * 0.5f - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun GestureTip(label: String, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

fun togglePlay(playback: PlaybackController, playing: Boolean) {
    if (playing) playback.pause() else playback.play()
}
