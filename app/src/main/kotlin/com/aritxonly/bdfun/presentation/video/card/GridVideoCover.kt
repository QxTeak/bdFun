package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 封面底部压暗，让白色播放量/时长在任何封面上都可读；常量复用，避免每张卡每次重组重建 Brush。 */
private val CoverScrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0x4D000000)))

@Composable
internal fun GridVideoCover(
    video: VideoItem,
    progress: Float,
    selectionEnabled: Boolean,
    selected: Boolean,
    reserveMeta: Boolean,
    blurred: Boolean = false,
    undoMessage: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    val blurProgress by animateFloatAsState(
        targetValue = if (blurred) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "grid_cover_blur",
    )
    // 撤销成功后 undoMessage 会立刻变 null；保留上一份文案，让提示跟着模糊一起淡出。
    var lastNotice by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(undoMessage) { if (undoMessage != null) lastNotice = undoMessage }
    val notice = undoMessage ?: lastNotice
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover, crossfade = false, size = 720),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                // 模糊半径跟着状态渐变，不再点击后一帧直接变糊。
                .then(
                    if (blurProgress > 0.01f) Modifier.blur((48f * blurProgress).dp) else Modifier,
                ),
        )

        if (notice != null && blurProgress > 0.01f) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = blurProgress }) {
                BlurredCoverNotice(notice)
            }
        }

        // 播放量/时长/充电/已看进度/勾选随模糊一起淡出，撤销时再淡回来。
        if (!blurred || blurProgress < 0.99f) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = 1f - blurProgress }) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(42.dp)
                        .background(CoverScrim),
                )

                GridCoverMetrics(video, reserveMeta)

                VideoChargeBadge(video.isCharge)

                // 历史记录：封面底部一条已看进度
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(MiuixTheme.colorScheme.primary),
                    )
                }

                GridCoverSelection(selectionEnabled, selected)
            }
        }
    }

}
