package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kyant.shapes.Capsule
import com.aritxonly.bdfun.core.model.feed.DynamicLiveRecommendation
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DynamicLiveCard(live: DynamicLiveRecommendation) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth().clip(ContinuousCornerShape(12.dp))
        .background(colors.surfaceContainerHigh).clickable(enabled = live.roomId > 0) {
            openDynamicLive(context, live.roomId)
        }) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            AsyncImage(model = rememberImageRequest(live.cover), contentDescription = live.title,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Text(if (live.live) "直播中" else "直播结束", color = colors.onPrimary,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(Capsule())
                    .background(colors.primary).padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp)
        }
        Text(live.title, color = colors.onSurfaceContainer, maxLines = 2, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp))
        Text(listOf(live.areaName, live.watched).filter { it.isNotBlank() }.joinToString(" · "),
            color = colors.onSurfaceVariantSummary, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
    }
}

/** Wide screens keep author navigation separate from the independently scrolling masonry feed. */
