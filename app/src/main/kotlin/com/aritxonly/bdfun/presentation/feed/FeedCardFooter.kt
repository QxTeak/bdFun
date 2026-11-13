package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.feed.FeedItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedCardFooter(item: FeedItem, liked: Boolean, actions: FeedCardActions) {
    val colors = MiuixTheme.colorScheme

    if (item.ipLocation.isNotBlank()) {
        Text(
            text = "IP属地：${item.ipLocation}",
            modifier = Modifier.padding(top = 8.dp),
            color = colors.onBackgroundVariant.copy(alpha = 0.72f),
            fontSize = 11.sp,
        )
    }

    Spacer(Modifier.height(6.dp))
    // 三个操作均分整行、各自居中：图标列和数量都严格对齐
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(
                    res = TeakIcon.Share,
                    tint = colors.onBackgroundVariant,
                    size = 18.dp,
                )
            },
            count = item.forward,
            onClick = actions.onRepost,
        )
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(
                    res = TeakIcon.CommentBubble,
                    tint = colors.onBackgroundVariant,
                    size = 18.dp,
                )
            },
            count = item.comment,
            onClick = actions.onComment,
        )
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(
                    res = if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                    tint = if (liked) BrandPink else colors.onBackgroundVariant,
                    size = 18.dp,
                )
            },
            count = item.like,
            tint = if (liked) BrandPink else colors.onBackgroundVariant,
            onClick = actions.onLike,
        )
    }
}
