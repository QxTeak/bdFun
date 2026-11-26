package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 封面底部压暗，让白色播放量/时长在任何封面上都可读；常量复用，避免每张卡每次重组重建 Brush。 */
private val CoverScrim = Brush.verticalGradient(listOf(Color.Transparent, Color(0x4D000000)))

@Composable
internal fun GridVideoCover(video: VideoItem, progress: Float, selectionEnabled: Boolean, selected: Boolean, reserveMeta: Boolean) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

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
