package com.aritxonly.bdfun.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 视频详情加载占位：一行行灰条，白色高光从左往右扫。
 *
 * 结构按真实元信息对齐：头像+昵称/粉丝行（右侧有关注胶囊）、两行标题、
 * 播放/弹幕/日期统计、AV 号、两行简介、分类，最后是五个操作圆钮。
 * 高光在**每个灰条自己的坐标系**里扫，条再短也能看到反光，不会整条变白。
 */
@Composable
internal fun VideoMetadataSkeleton(modifier: Modifier = Modifier) {
    val sweep = rememberSkeletonSweep()
    val base = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.10f)
    val highlight = skeletonHighlight()
    val fill = Modifier.skeletonFill(sweep, base, highlight)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        // 作者行：头像 + 昵称/副标题 + 右侧关注胶囊。
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).then(fill))
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.width(96.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).then(fill))
                Box(Modifier.width(72.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).then(fill))
            }
            Box(Modifier.width(56.dp).height(26.dp).clip(RoundedCornerShape(50)).then(fill))
        }
        Spacer(Modifier.height(10.dp))
        // 标题两行（字号 17sp / 行高 24sp）。
        Box(Modifier.fillMaxWidth().height(24.dp).clip(RoundedCornerShape(5.dp)).then(fill))
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth(0.62f).height(24.dp).clip(RoundedCornerShape(5.dp)).then(fill))
        Spacer(Modifier.height(8.dp))
        // 播放 / 弹幕 / 日期统计行。
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.width(64.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).then(fill))
            Box(Modifier.width(56.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).then(fill))
            Box(Modifier.width(76.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).then(fill))
        }
        Spacer(Modifier.height(10.dp))
        // AV 号 / 分P 标识行。
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.width(64.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).then(fill))
            Box(Modifier.width(44.dp).height(18.dp).clip(RoundedCornerShape(9.dp)).then(fill))
        }
        Spacer(Modifier.height(10.dp))
        // 简介两行。
        Box(Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(4.dp)).then(fill))
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth(0.78f).height(13.dp).clip(RoundedCornerShape(4.dp)).then(fill))
        Spacer(Modifier.height(8.dp))
        // 分类。
        Box(Modifier.width(72.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).then(fill))
        Spacer(Modifier.height(16.dp))
        // 点赞 / 不喜欢 / 投币 / 收藏 / 分享，五个等宽列居中。
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(5) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(30.dp).clip(CircleShape).then(fill))
                }
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
    // 深色主题用更亮的灰做扫光，浅色主题才用白光；两者都保持"条还在"而不是被扫没。
    return if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.85f)
}

private fun Modifier.skeletonFill(progress: Float, base: Color, highlight: Color): Modifier =
    drawWithCache {
        val band = size.width * 0.32f
        val x = -band + progress * (size.width + band * 2f)
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x - band, 0f),
            end = Offset(x + band, 0f),
        )
        onDrawBehind { drawRect(brush) }
    }

