package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LargeVideoCover(video: VideoItem, progress: Float) {
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
                .height(48.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x66000000))),
                ),
        )
        LargeCoverMetrics(video)
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
