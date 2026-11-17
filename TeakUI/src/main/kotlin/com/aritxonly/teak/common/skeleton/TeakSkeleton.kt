package com.aritxonly.teak.common.skeleton

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 全局共用的骨架屏：灰色块 + 一道白色高光从左往右扫。
 *
 * 高光在**每个灰块自己的坐标系**里扫，短条也能看到反光；灰色块的颜色只在这里定义，
 * 各页面只负责按自己的真实布局摆位置。视频详情、首页、直播、番剧、搜索、评论
 * 都复用这一套，避免每处再写一遍扫光进度。
 */
@Composable
fun rememberTeakSkeletonSweep(): Float {
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            // project 里其余手写补间都走 delay，不依赖 frame clock。
            delay(16)
            progress = (progress + 0.012f) % 1f
        }
    }
    return progress
}

/** 骨架灰块的基准色；浅色主题略深，深色主题略亮，保证块始终"看得见"。 */
@Composable
fun teakSkeletonBase(): Color = MiuixTheme.colorScheme.onBackground.copy(alpha = 0.10f)

/** 扫光高光：深色主题用更亮的灰，浅色主题才用白光。 */
@Composable
fun teakSkeletonHighlight(): Color {
    val isDark = MiuixTheme.colorScheme.onBackground.luminance() > 0.5f
    return if (isDark) Color.White.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.85f)
}

/** 给任意灰块套上扫光；调用方先决定尺寸和形状。 */
@Composable
fun Modifier.teakSkeleton(
    progress: Float,
    shape: Shape = RoundedCornerShape(4.dp),
): Modifier {
    val base = teakSkeletonBase()
    val highlight = teakSkeletonHighlight()
    return this.clip(shape).drawWithCache {
        val band = size.width * 0.32f
        val x = -band + progress * (size.width + band * 2f)
        val brush = Brush.linearGradient(
            colors = listOf(base, highlight, base),
            start = Offset(x - band, 0f),
            end = Offset(x + band, 0f),
        )
        onDrawBehind { drawRect(brush) }
    }
}

/** 最常用的组合：占位灰块（默认 4dp 圆角）。 */
@Composable
fun TeakSkeletonBlock(
    progress: Float,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(4.dp),
) {
    Box(modifier = modifier.teakSkeleton(progress, shape))
}

/**
 * 通用视频/直播卡片骨架：封面 + 两行标题 + 底部元信息。
 *
 * 不画卡片底色，交给调用方决定放在网格还是列表里；封面比例默认 16:9，
 * 番剧卡传 3:4 即可。
 */
@Composable
fun TeakSkeletonVideoCard(
    progress: Float,
    modifier: Modifier = Modifier,
    coverAspectRatio: Float = 16f / 9f,
    coverShape: Shape = RoundedCornerShape(12.dp),
    titleHeight: androidx.compose.ui.unit.Dp = 13.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        TeakSkeletonBlock(
            progress,
            Modifier.fillMaxWidth().aspectRatio(coverAspectRatio),
            coverShape,
        )
        Spacer(Modifier.height(8.dp))
        TeakSkeletonBlock(progress, Modifier.fillMaxWidth().height(titleHeight))
        Spacer(Modifier.height(6.dp))
        TeakSkeletonBlock(progress, Modifier.fillMaxWidth(0.66f).height(titleHeight))
        Spacer(Modifier.height(8.dp))
        TeakSkeletonBlock(progress, Modifier.fillMaxWidth(0.38f).height(10.dp))
    }
}

/** 横向视频行骨架：封面 + 两行标题 + 底部元信息，和详情相关推荐/搜索结果同构。 */
@Composable
fun TeakSkeletonVideoRow(
    progress: Float,
    modifier: Modifier = Modifier,
    coverWidth: androidx.compose.ui.unit.Dp = 150.dp,
) {
    androidx.compose.foundation.layout.Row(modifier = modifier.fillMaxWidth()) {
        TeakSkeletonBlock(
            progress,
            Modifier.width(coverWidth).aspectRatio(16f / 9f),
            RoundedCornerShape(12.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            TeakSkeletonBlock(progress, Modifier.fillMaxWidth().height(14.dp))
            Spacer(Modifier.height(7.dp))
            TeakSkeletonBlock(progress, Modifier.fillMaxWidth(0.72f).height(14.dp))
            Spacer(Modifier.height(30.dp))
            TeakSkeletonBlock(progress, Modifier.width(92.dp).height(11.dp))
        }
    }
}
