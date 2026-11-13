package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedVideoCard(
    video: VideoItem,
    isCollection: Boolean,
    autoPlay: Boolean,
    muted: Boolean,
    onOpenVideo: (VideoItem) -> Unit,
    onToggleMute: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(ContinuousCornerShape(12.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) { onOpenVideo(video) },
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (autoPlay) {
            InlineVideoPlayer(
                video = video,
                muted = muted,
                modifier = Modifier.fillMaxSize(),
            )
        }
        FeedVideoCollectionBadge(isCollection)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(46.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x66000000)),
                    ),
                ),
        )
        FeedVideoMetrics(video, muted, onToggleMute)
    }
    Text(
        text = video.title,
        color = colors.onBackground,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 8.dp),
    )
}
