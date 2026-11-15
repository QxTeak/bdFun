package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.model.comment.CommentItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue

@Composable
internal fun CommentThreadReplyCard(
    reply: CommentItem, index: Int, lastIndex: Int, end: Boolean,
    transient: CommentThreadPresentationStateHolder, callbacks: ThreadSheetCallbacks,
) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    val colors = MiuixTheme.colorScheme
    val rowBackground by androidx.compose.animation.animateColorAsState(
        targetValue = if (reply.rpid == transient.flashRpid) {
            colors.onBackground.copy(alpha = 0.10f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = androidx.compose.animation.core.tween(120),
        label = "thread-reply-flash",
    )
    // 与主评论区结构一致：平铺评论行，没有圆角卡片；只保留「刚发出的回复」那一下高亮。
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground),
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
