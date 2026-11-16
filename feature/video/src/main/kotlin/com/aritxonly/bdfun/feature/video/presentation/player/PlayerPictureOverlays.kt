package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.kyant.shapes.Capsule

@Composable
fun BoxScope.PlayerAlwaysProgress(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean, positionMs: Long, durationMs: Long,
    progressBarMode: Int, accentColor: Color,
) {
    val controlsVisible by ui.controlsVisible
    val fsAudioOnly by ui.fsAudioOnly
    // 进度条常显：控件整体隐藏时，在画面最底部留一条 1dp 的粉色进度。
    val alwaysProgress = !controlsVisible && !fsAudioOnly &&
        (progressBarMode == 3 ||
            (progressBarMode == 1 && !fullscreen) ||
            (progressBarMode == 2 && fullscreen))
    if (alwaysProgress && durationMs > 0L) {
        val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(accentColor),
        )
    }

}

@Composable
fun BoxScope.PlayerHotDanmakuOverlay(
    ui: VideoPlayerUiStateHolder,
    fullscreen: Boolean,
    hotDanmakuEnabled: Boolean,
    strokeDp: Float = 1.5f,
    textScale: Float = 1f,
) {
    val hotDanmaku by ui.hotDanmaku
    val fsAudioOnly by ui.fsAudioOnly
    // 弹幕计数：和普通弹幕使用同一套原生绘制方式（同描边设置），
    // 不再用两层 Compose Text 叠描边。播放控件打开时也保持显示。
    if (hotDanmakuEnabled && hotDanmaku.isNotEmpty() && !fsAudioOnly) {
        val density = LocalDensity.current
        // 计数比普通弹幕大一号，文字和数字都倾斜；描边仍沿用弹幕设置的宽度。
        val textSizePx = with(density) { (16.sp * textScale).toPx() }
        val countSizePx = with(density) { (20.sp * textScale).toPx() }
        val strokePx = with(density) { strokeDp.dp.toPx() }
        val gapPx = with(density) { 24.dp.toPx() }
        val canvasHeight = with(density) { ((countSizePx * 1.6f).coerceAtLeast(textSizePx * 1.3f)).toDp() }
        // Paint / 字体测量对象只创建一次；Canvas 重绘时不再每帧 new Paint。
        val textPaint = remember(textSizePx) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.DITHER_FLAG).apply {
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.ITALIC)
                textSize = textSizePx
            }
        }
        val countPaint = remember(countSizePx) {
            android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.DITHER_FLAG).apply {
                typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.BOLD_ITALIC)
                textSize = countSizePx
            }
        }
        val ellipsizePaint = remember(textSizePx) { android.text.TextPaint() }
        val baselineOffset = remember(countSizePx) { -countPaint.fontMetrics.top }
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(top = if (fullscreen) 10.dp else 4.dp)
                .padding(horizontal = 12.dp)
                .height(canvasHeight),
        ) {
            drawIntoCanvas { canvas ->
                drawHotDanmaku(canvas.nativeCanvas, hotDanmaku, size.width, textPaint, countPaint,
                    ellipsizePaint, baselineOffset, strokePx, gapPx)
            }
        }
    }
}

/**
 * 与弹幕引擎完全相同的绘制顺序：先按 [strokePx] 和 0xE6000000 描边，再填白色。
 * 正文和数字都倾斜，数字更大并紧跟正文。
 */
private fun drawHotDanmaku(
    canvas: android.graphics.Canvas,
    items: List<Pair<String, Long>>,
    availableWidth: Float,
    textPaint: android.graphics.Paint,
    countPaint: android.graphics.Paint,
    ellipsizePaint: android.text.TextPaint,
    baselineOffset: Float,
    strokePx: Float,
    gapPx: Float,
) {
    // 数字紧跟在正文后面，只留一点字距，不再按半屏宽度把两者分开。
    val numberGapPx = countPaint.textSize * 0.08f
    val maxItemWidth = ((availableWidth - gapPx) / 2f).coerceAtLeast(1f)
    val rows = items.map { (text, count) ->
        val countText = "×$count"
        val countWidth = countPaint.measureText(countText)
        val textLimit = (maxItemWidth - countWidth - numberGapPx).coerceAtLeast(1f)
        ellipsizePaint.typeface = textPaint.typeface
        ellipsizePaint.textSize = textPaint.textSize
        val display = android.text.TextUtils.ellipsize(
            text,
            ellipsizePaint,
            textLimit,
            android.text.TextUtils.TruncateAt.END,
        ).toString()
        HotDanmakuRow(display, countText, textPaint.measureText(display), countWidth)
    }
    val totalWidth = rows.sumOf { (it.textWidth + it.countWidth).toDouble() }.toFloat() +
        numberGapPx * rows.size +
        gapPx * (rows.size - 1).coerceAtLeast(0)
    var x = (availableWidth - totalWidth) / 2f
    rows.forEachIndexed { index, row ->
        drawHotDanmakuText(canvas, row.text, x, baselineOffset, textPaint, strokePx)
        x += row.textWidth + numberGapPx
        drawHotDanmakuText(canvas, row.count, x, baselineOffset, countPaint, strokePx)
        x += row.countWidth
        if (index != rows.lastIndex) x += gapPx
    }
}

private fun drawHotDanmakuText(
    canvas: android.graphics.Canvas,
    text: String,
    x: Float,
    baseline: Float,
    paint: android.graphics.Paint,
    strokePx: Float,
) {
    if (strokePx > 0f) {
        paint.style = android.graphics.Paint.Style.STROKE
        paint.color = 0xE6000000.toInt()
        paint.strokeWidth = strokePx
        canvas.drawText(text, x, baseline, paint)
    }
    paint.style = android.graphics.Paint.Style.FILL
    paint.color = android.graphics.Color.WHITE
    paint.strokeWidth = 0f
    canvas.drawText(text, x, baseline, paint)
}

private data class HotDanmakuRow(
    val text: String,
    val count: String,
    val textWidth: Float,
    val countWidth: Float,
)

@Composable
fun BoxScope.PlayerSponsorOverlay(
    activeSponsor: SponsorSegment?, fullscreen: Boolean, accentColor: Color, onSkip: (SponsorSegment) -> Unit,
) {
    // 空降助手：进入赞助片段时在右下角出现跳过按钮。
    activeSponsor?.let { segment ->
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (fullscreen) 104.dp else 62.dp,
                )
                .clip(Capsule())
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(interactionSource = null, indication = null) {
                    onSkip(segment)
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "空降助手 · ${sponsorCategoryLabel(segment.category)}",
                color = Color.White,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "跳过",
                color = accentColor,
                fontSize = 12.sp,
            )
        }
    }
}
