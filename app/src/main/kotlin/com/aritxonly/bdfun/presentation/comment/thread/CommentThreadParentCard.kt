package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentThreadParentCard(comment: CommentItem, transient: CommentThreadPresentationStateHolder, callbacks: ThreadSheetCallbacks) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceContainer),
    ) {
        CommentRow(
            item = comment.copy(
                replyCount = 0,
                extras = comment.extras.copy(previewReplies = emptyList()),
                liked = likes.isLiked(comment),
                like = likes.likeCount(comment),
            ),
            disliked = transient.dislikedIds[comment.rpid] == true,
            actions = CommentRowActions(
                onLike = { callbacks.onLike(comment) },
                onDislike = {
                    transient.dislikedIds[comment.rpid] =
                        transient.dislikedIds[comment.rpid] != true
                },
                onReply = { transient.replyTarget = comment },
                onLongPress = { bounds ->
                    transient.menuAnchor = bounds
                    transient.menuReply = comment
                    transient.menuVisible = true
                },
                onMore = { bounds ->
                    transient.menuAnchor = bounds
                    transient.menuReply = comment
                    transient.menuVisible = true
                },
                onOpenReplies = {},
                onReplyToReply = { transient.replyTarget = it },
                onLongPressReply = { reply, bounds ->
                    transient.menuAnchor = bounds
                    transient.menuReply = reply
                    transient.menuVisible = true
                },
                onPreviewLike = { callbacks.onLike(it) },
            ),
        )
    }
}
