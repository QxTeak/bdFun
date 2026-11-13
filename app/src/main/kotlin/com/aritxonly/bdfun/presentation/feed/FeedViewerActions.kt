package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.bdfun.core.model.feed.FeedItem

@Composable
internal fun FeedViewerActions(item: FeedItem, liked: Boolean, onLike: () -> Unit, onComment: () -> Unit, onRepost: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(TeakIcon.Share, Color.White, size = 20.dp)
            },
            count = item.forward,
            tint = Color.White,
            onClick = onRepost,
        )
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(TeakIcon.CommentBubble, Color.White, size = 20.dp)
            },
            count = item.comment,
            tint = Color.White,
            onClick = onComment,
        )
        FeedAction(
            modifier = Modifier.weight(1f),
            icon = {
                ResourceGlyph(
                    if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
                    if (liked) BrandPink else Color.White,
                    size = 20.dp,
                )
            },
            count = item.like,
            tint = if (liked) BrandPink else Color.White,
            onClick = onLike,
        )
    }
}
