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

/** 大卡封面底部压暗；常量复用，避免每次重组重建 Brush。 */
private val LargeCoverScrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000)))

@Composable
internal fun LargeVideoCover(
    video: VideoItem, progress: Float, blurred: Boolean = false, undoMessage: String? = null,
) {
    val colors = MiuixTheme.colorScheme
    val blurProgress by animateFloatAsState(
        targetValue = if (blurred) 1f else 0f,
        animationSpec = tween(280, easing = FastOutSlowInEasing),
        label = "large_cover_blur",
    )
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
            model = rememberImageRequest(video.cover, crossfade = false, size = 1080),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurProgress > 0.01f) Modifier.blur((48f * blurProgress).dp) else Modifier,
                ),
        )
        if (notice != null && blurProgress > 0.01f) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = blurProgress }) {
                BlurredCoverNotice(notice)
            }
        }
        if (!blurred || blurProgress < 0.99f) {
            Box(Modifier.matchParentSize().graphicsLayer { alpha = 1f - blurProgress }) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(LargeCoverScrim),
                )
                LargeCoverMetrics(video)
                VideoChargeBadge(video.isCharge)
                if (progress > 0f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(2.dp)
                            .background(colors.primary),
                    )
                }
            }
        }
    }
}
