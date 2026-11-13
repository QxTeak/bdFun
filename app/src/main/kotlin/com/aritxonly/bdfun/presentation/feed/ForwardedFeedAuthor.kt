package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.feed.FeedItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ForwardedFeedAuthor(item: FeedItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = rememberImageRequest(item.face),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(colors.surfaceContainerHighest)
                .clickable(interactionSource = null, indication = null) {
                    if (item.mid > 0L) {
                        navigateToSpace(context, item.mid)
                    }
                },
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = item.name,
            color = if (item.authorVip) colors.primary else colors.onBackground,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clickable(
                interactionSource = null,
                indication = null,
            ) {
                if (item.mid > 0L) {
                    navigateToSpace(context, item.mid)
                }
            },
        )
        Spacer(Modifier.weight(1f))
        Text(text = item.time, color = colors.onBackgroundVariant, fontSize = 10.sp)
    }
}
