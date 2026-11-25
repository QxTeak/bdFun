package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.rememberManualProgress

import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoPage
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun VideoPageStrip(
    pages: List<com.aritxonly.bdfun.core.model.video.VideoPage>,
    currentCid: Long,
    onSelect: (com.aritxonly.bdfun.core.model.video.VideoPage) -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val stripState = rememberLazyListState()
    val leftAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stripState.canScrollBackward) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "pageStripLeftMask",
    )
    val rightAlpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (stripState.canScrollForward) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(180),
        label = "pageStripRightMask",
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = stripState,
            modifier = Modifier.fillMaxWidth(),
            // 外层已经有 16dp；这里不再加，让第一个 P 的左边和合集圆角卡片左边缘对齐。
            contentPadding = PaddingValues(horizontal = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(items = pages, key = { it.cid }) { page ->
                val active = page.cid == currentCid
                Box(
                    modifier = Modifier
                        .width(150.dp)
                        .height(54.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(LocalTeakContentColors.current.groupedBackground)
                        .clickable(interactionSource = null, indication = null) { onSelect(page) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = page.part.ifBlank { "P${page.page}" },
                        color = if (active) colors.primary else colors.onBackground,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        // 左右各 6dp 渐变，提示这里还能横向滑动；到最左/最右才淡出。
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(6.dp)
                .height(54.dp)
                .graphicsLayer { alpha = leftAlpha }
                .background(
                    Brush.horizontalGradient(
                        listOf(LocalTeakContentColors.current.background, LocalTeakContentColors.current.background.copy(alpha = 0f)),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(6.dp)
                .height(54.dp)
                .graphicsLayer { alpha = rightAlpha }
                .background(
                    Brush.horizontalGradient(
                        listOf(LocalTeakContentColors.current.background.copy(alpha = 0f), LocalTeakContentColors.current.background),
                    ),
                ),
        )
    }
}

/**
 * 播放器下方内容按从上到下依次浮出。
 * 用户反馈原来上浮拖沓：这里把错开间隔和单段时长都砍半
 * （60ms / 120ms → 30ms / 60ms），保留层次但整体更快。
 */
@Composable
fun rememberStaggerProgress(ready: Boolean, order: Int): Float {
    var target by remember { mutableStateOf(0f) }
    LaunchedEffect(ready, order) {
        if (!ready) {
            target = 0f
        } else {
            kotlinx.coroutines.delay(order * 30L)
            target = 1f
        }
    }
    return rememberManualProgress(target, 60f)
}
