package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Sheet 顶部那根拖拽条。
 *
 * 抽出来给两种宿主复用：
 * - 底部 Sheet 的 [com.aritxonly.teak.common.modal.sheet] 头部；
 * - 动态 UP 页顶部的下拉退出手势。
 *
 * 尺寸/透明度/按压反馈都是同一套常量，改观感只改这里。
 */
@Composable
fun TeakGrabberBar(
    modifier: Modifier = Modifier,
    width: Dp = 45.dp,
    pressed: Boolean = false,
    color: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
) {
    val alpha = lerp(0.2f, 0.35f, if (pressed) 1f else 0f)
    Box(
        modifier = modifier
            .width(width)
            .height(4.dp)
            .graphicsLayer { scaleY = if (pressed) 1.15f else 1f }
            .clip(RoundedCornerShape(2.dp))
            .background(color.copy(alpha = alpha)),
    )
}

/** 拖拽条所在的可点击/可拖拽区域高度。 */
val TeakGrabberStripHeight: Dp = 24.dp
