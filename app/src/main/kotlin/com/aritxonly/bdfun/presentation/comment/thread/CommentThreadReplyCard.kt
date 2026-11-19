package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.model.comment.CommentItem

@Composable
internal fun CommentThreadReplyCard(
    reply: CommentItem, index: Int, lastIndex: Int, end: Boolean,
    transient: CommentThreadPresentationStateHolder, callbacks: ThreadSheetCallbacks,
) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    // 与主评论区一致：平铺评论行，没有圆角卡片；高亮从楼中楼共享目标里判断。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .commentFlash(reply.rpid),
    ) {
        CommentRow(
            item = reply.copy(
                liked = likes.isLiked(reply),
                like = likes.likeCount(reply),
            ),
            disliked = transient.dislikedIds[reply.rpid] == true,
            actions = CommentRowActions(
                onLike = { callbacks.onLike(reply) },
                onDislike = {
                    transient.dislikedIds[reply.rpid] =
                        transient.dislikedIds[reply.rpid] != true
                },
                onReply = { transient.replyTarget = reply },
                onLongPress = { bounds ->
                    transient.menuAnchor = bounds
                    transient.menuReply = reply
                    transient.menuVisible = true
                },
                onMore = { bounds ->
                    transient.menuAnchor = bounds
                    transient.menuReply = reply
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
