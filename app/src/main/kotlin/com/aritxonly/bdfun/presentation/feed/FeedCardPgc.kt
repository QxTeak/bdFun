package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedCardPgc(item: FeedItem, onOpen: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    if (item.pgcCover.isNotBlank()) {
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(ContinuousCornerShape(12.dp))
                .background(colors.surfaceContainerHigh)
                .clickable(interactionSource = null, indication = null, onClick = onOpen),
        ) {
            AsyncImage(
                model = rememberImageRequest(item.pgcCover),
                contentDescription = item.pgcTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 8.dp, end = 8.dp)
                    .width(42.dp)
                    .clip(Capsule())
                    .background(colors.primary.copy(alpha = 0.92f))
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = item.pgcLabel.ifBlank { "剧集" },
                    color = Color.White,
                    fontSize = 9.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
        }
        if (item.pgcTitle.isNotBlank()) {
            Text(
                text = item.pgcTitle,
                color = colors.onBackground,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }

}
