package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 视频详情加载占位：一行行灰条，白色高光从左往右扫。
 *
 * 替掉原来的「内容依次上浮」：加载时先有稳定的骨架，数据到了直接替换，
 * 不再让整块内容从下方浮出。
 */
@Composable
internal fun VideoMetadataSkeleton(modifier: Modifier = Modifier) {
    val sweep = rememberSkeletonSweep()
    val fill = MiuixTheme.colorScheme.surfaceContainerHigh
    val highlight = skeletonHighlight()
    val brush = skeletonBrush(sweep, fill, highlight)
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(brush))
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(96.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Box(Modifier.width(64.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth().height(17.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.62f).height(17.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.78f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
            repeat(5) {
                Box(Modifier.size(30.dp).clip(CircleShape).background(brush))
            }
        }
    }
}

/** 用 delay 推进，和项目里其余手写补间保持一致（这里不依赖 frame clock）。 */
@Composable
private fun rememberSkeletonSweep(): Float {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            progress = (progress + 0.012f) % 1f
        }
    }
    return progress
}

@Composable
private fun skeletonHighlight(): Color {
    val isDark = MiuixTheme.colorScheme.onBackground.luminance() > 0.5f
    return if (isDark) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.62f)
}

@Composable
private fun skeletonBrush(progress: Float, fill: Color, highlight: Color): Brush {
    val widthPx = with(LocalDensity.current) { 520.dp.toPx() }
    // 高光带从左侧屏外扫到右侧屏外，所有灰条共用同一道扫光，观感是一整块骨架。
    val startX = (progress * 2.4f - 0.7f) * widthPx
    return Brush.linearGradient(
        colors = listOf(fill, highlight, fill),
        start = Offset(startX, 0f),
        end = Offset(startX + widthPx * 0.45f, 0f),
    )
}
