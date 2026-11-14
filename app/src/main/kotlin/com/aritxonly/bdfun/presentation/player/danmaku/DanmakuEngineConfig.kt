package com.aritxonly.bdfun.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.purebilibili.danmaku.engine.DanmakuRenderConfig


internal const val BASE_TRAVEL_MS = 8_500f
internal const val FIXED_SHOW_MS = 4_000L
@Composable
internal fun danmakuEngineConfig(drawing: DanmakuLayerDrawing, viewSize: IntSize, playbackSpeed: Float): DanmakuRenderConfig {
    val density = LocalDensity.current
    val baseTextPx = with(density) { 12.sp.toPx() } * drawing.fontScale
    val strokePx = with(density) { drawing.stroke.dp.toPx() }
    val area = drawing.area
    val speed = drawing.speed
    val subtitleGuard = drawing.subtitleGuard
    val portraitGuard = drawing.portraitGuard
    val typeface = remember { Typeface.create("sans-serif-medium", Typeface.NORMAL) }
    val lineHeightPx = (baseTextPx * 1.9f).coerceAtLeast(1f)
    val heightPx = viewSize.height.toFloat()
    // 顶部让给「弹幕计数」的高度（已开启计数时非 0）：行数要按剩余高度算，
    // 否则最下面几行会被顶出画面。
    val topGuardPx = with(density) { drawing.topGuardDp.dp.toPx() }
    val lineCount = if (heightPx > 0f) {
        // 一页最多 9 行；行高加大后上下弹幕不会挤在一起。
        ((((heightPx * area) - topGuardPx).coerceAtLeast(lineHeightPx)) / lineHeightPx)
            .toInt()
            .coerceIn(1, 9)
    } else {
        1
    }
    val bottomMarginPx = with(density) {
        (64.dp + if (subtitleGuard) 22.dp else 0.dp).toPx()
    }
    return DanmakuRenderConfig(
        alpha = 255,
        textSizePx = baseTextPx,
        typeface = typeface,
        strokeWidthPx = strokePx,
        strokeColor = 0xE6000000.toInt(),
        // 弹幕速度设置 + 视频倍速：引擎自己按真实时间走，时长要一起缩放。
        scrollDurationMs = (BASE_TRAVEL_MS / (speed * playbackSpeed.coerceAtLeast(0.1f)))
            .toLong()
            .coerceAtLeast(1L),
        lineHeightPx = lineHeightPx,
        lineMarginPx = 0f,
        lineCount = lineCount,
        // 弹幕从计数下面开始排，不和顶部的计数叠在一起。
        topMarginPx = topGuardPx,
        bottomMarginPx = bottomMarginPx,
        pinnedDurationMs = FIXED_SHOW_MS,
        playSpeedPercent = 100,
        maskEnabled = portraitGuard,
    )

}
