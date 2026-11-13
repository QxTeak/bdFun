package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.rememberImageRequest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.user.SpaceSeason
import com.aritxonly.bdfun.ui.formatCount
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SpaceCollectionGridCard(
    season: SpaceSeason,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(com.aritxonly.bdfun.ui.FeedCardShape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(colors.surfaceContainerHigh),
        ) {
            if (season.cover.isNotBlank()) {
                AsyncImage(
                    model = rememberImageRequest(season.cover, crossfade = false),
                    contentDescription = season.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(42.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color(0x4D000000)),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(start = 6.dp, end = 6.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${formatCount(season.total.toLong())} 个视频",
                    color = Color.White,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
            }
        }
        Text(
            text = season.title,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            lineHeight = 18.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp),
        )
    }
}
