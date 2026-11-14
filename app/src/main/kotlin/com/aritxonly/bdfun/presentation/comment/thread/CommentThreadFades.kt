package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.CommentThreadFades(
    background: androidx.compose.ui.graphics.Color, headerHeight: androidx.compose.ui.unit.Dp,
    transient: CommentThreadPresentationStateHolder, callbacks: ThreadSheetCallbacks,
) {
    val colors = MiuixTheme.colorScheme
    // 顶部渐变从「评论详情」标题的垂直中线开始，往下渐隐到透明；
    // 标题固定在最上面，画在渐变之上，不会被内容盖住。
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(headerHeight)
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to background,
                        0.5f to background,
                        1f to background.copy(alpha = 0f),
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(headerHeight)
            .padding(start = 16.dp, end = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = "评论详情",
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
        )
        // 右上角关闭键：和返回键同一个效果、同一套尺寸比例（40dp 热区 + 22dp 图标）。
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .clip(CircleShape)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { callbacks.onDismiss() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Close,
                contentDescription = "关闭",
                tint = colors.onBackgroundVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(54.dp)
            .background(
                Brush.verticalGradient(
                    listOf(background.copy(alpha = 0f), background),
                ),
            ),
    )
    // 回复框打开时，点空白区域只退出回复框，不关闭展开页。
    if (transient.replyTarget != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { callbacks.onExitReply() },
                ),
        )
    }
}
