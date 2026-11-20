package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoRelatedCover(item: VideoItem, coverWidth: androidx.compose.ui.unit.Dp, coverHeight: androidx.compose.ui.unit.Dp) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .width(coverWidth)
            .height(coverHeight)
            .clip(ContinuousCornerShape(10.dp))
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(item.cover, crossfade = false),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // 时长直接压在封面上，不加任何背景
        if (item.duration > 0) {
            Text(
                text = formatDuration(item.duration),
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
internal fun VideoRelatedMetadata(
    item: VideoItem, metaLabel: String?, coverHeight: androidx.compose.ui.unit.Dp, modifier: Modifier,
) {
    val colors = MiuixTheme.colorScheme
    // 右列高度只给下限：字号放大或两行标题时能撑高，UP 行不会被挤出容器裁掉。
    Column(
        modifier = modifier.heightIn(min = coverHeight),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = item.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.padding(start = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MetricGlyph(play = true, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(item.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
            }
        }
        if (metaLabel != null) {
            Text(
                text = metaLabel,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = item.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
