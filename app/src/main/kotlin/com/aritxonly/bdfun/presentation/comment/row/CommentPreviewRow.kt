package com.aritxonly.bdfun.ui

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import androidx.compose.runtime.getValue

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun CommentPreviewRow(reply: CommentItem, actions: CommentPreviewActions) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    val ui = remember(reply.rpid) { CommentRowPresentationStateHolder() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { ui.bounds = it.boundsInWindow() }
            .clip(RoundedCornerShape(8.dp))
            .commentFlash(reply.rpid, RoundedCornerShape(8.dp))
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onClick = { actions.onReplyToReply(reply) },
                onLongClick = { actions.onLongPressReply(reply, ui.bounds) },
            )
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // 同样只订阅这条折叠回复自己的值。
        val liked by remember(reply.rpid, reply.liked) {
            derivedStateOf { likes.likedMap[reply.rpid] ?: reply.liked }
        }
        val likeCount by remember(reply.rpid, reply.like) {
            derivedStateOf { likes.countMap[reply.rpid] ?: reply.like }
        }
        CommentPreviewText(reply, liked, likeCount, actions)
    }
}
