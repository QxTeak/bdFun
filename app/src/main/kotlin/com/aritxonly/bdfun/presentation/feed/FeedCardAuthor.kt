package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedCardAuthor(
    item: FeedItem,
    live: Boolean = false,
    moreMorphState: com.aritxonly.teak.common.navigation.TeakMorphMenuState? = null,
    onMore: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp)) {
            AsyncImage(
                model = rememberImageRequest(item.face),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(colors.surfaceContainerHigh)
                    .clickable(interactionSource = null, indication = null) {
                        if (item.mid > 0L) {
                            navigateToSpace(context, item.mid)
                        }
                    },
            )
            if (live) {
                // 动态卡片里只保留「直播中」胶囊，不再加静态粉圈；圆圈动效只属于顶部头像行。
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 2.dp)
                        .clip(Capsule())
                        .background(Color.White)
                        .padding(0.8.dp)
                        .clip(Capsule())
                        .background(colors.primary)
                        .padding(horizontal = 3.dp, vertical = 0.5.dp),
                ) {
                    Text(
                        text = "直播中",
                        color = Color.White,
                        fontSize = 6.5.sp,
                        lineHeight = 7.5.sp,
                        maxLines = 1,
                    )
                }
            }
        }
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = if (item.authorVip) colors.primary else colors.onBackground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .offset(y = 2.dp)
                    .clickable(interactionSource = null, indication = null) {
                        if (item.mid > 0L) {
                            navigateToSpace(context, item.mid)
                        }
                    },
            )
            Text(
                text = item.time,
                color = colors.onBackgroundVariant,
                fontSize = 11.sp,
                modifier = Modifier.offset(x = 1.dp),
            )
        }
        // 省略号贴卡片最右；横排垂直居中，恰好和左边头像同高。
        if (moreMorphState != null) {
            VideoCardMoreButton(
                onClick = onMore ?: {},
                touchSize = 24.dp,
                iconSize = 16.dp,
                morphState = moreMorphState,
                onMorphTrigger = onMore,
            )
        }
    }

}
