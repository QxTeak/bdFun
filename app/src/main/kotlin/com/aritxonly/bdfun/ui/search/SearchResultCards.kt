package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchUserRow(user: SearchUser, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = rememberImageRequest(user.face),
            contentDescription = user.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(colors.surfaceContainerHigh),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = user.name,
                color = colors.onBackground,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (user.sign.isNotBlank()) {
                Text(
                    text = user.sign,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = "${formatCount(user.fans)} 粉丝 · ${user.videos} 投稿",
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
internal fun SearchResultRow(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val coverWidth = 150.dp
    val coverHeight = coverWidth * 9f / 16f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        SearchVideoCover(video, coverWidth, coverHeight)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f).height(coverHeight)) {
            Text(
                text = video.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Normal,
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
                    text = formatCount(video.play),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
                Spacer(Modifier.width(6.dp))
                MetricGlyph(play = false, color = colors.onBackgroundVariant, size = 11.dp)
                Spacer(Modifier.width(2.dp))
                Text(
                    text = formatCount(video.danmaku),
                    color = colors.onBackgroundVariant,
                    fontSize = 10.sp,
                )
            }
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                UpBadge()
                Spacer(Modifier.width(5.dp))
                Text(
                    text = video.ownerName,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun SearchVideoCover(video: VideoItem, coverWidth: androidx.compose.ui.unit.Dp, coverHeight: androidx.compose.ui.unit.Dp) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .width(coverWidth)
            .height(coverHeight)
            .clip(ContinuousCornerShape(10.dp))
            .background(colors.surfaceContainerHigh),
    ) {
        AsyncImage(
            model = rememberImageRequest(video.cover),
            contentDescription = video.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        if (video.duration > 0) {
            Text(
                text = formatDuration(video.duration),
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
