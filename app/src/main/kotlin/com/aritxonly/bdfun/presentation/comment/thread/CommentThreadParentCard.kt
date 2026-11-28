package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.core.model.comment.CommentItem

@Composable
internal fun CommentThreadParentCard(comment: CommentItem, transient: CommentThreadPresentationStateHolder, callbacks: ThreadSheetCallbacks) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    // 与主评论区结构一致：直接平铺一行评论，不再套圆角矩形背景。
    // 定位失败时也会退化为闪这一张根评论卡片。
    Box(Modifier.fillMaxWidth().commentFlash(comment.rpid)) {
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
            onLongPress = { bounds, _ ->
                transient.menuAnchor = bounds
                transient.menuReply = comment
                transient.menuVisible = true
            },
            onMore = { bounds, _ ->
                transient.menuAnchor = bounds
                transient.menuReply = comment
                transient.menuVisible = true
            },
            onOpenReplies = {},
            onReplyToReply = { transient.replyTarget = it },
            onLongPressReply = { reply, bounds, _ ->
                transient.menuAnchor = bounds
                transient.menuReply = reply
                transient.menuVisible = true
            },
            onPreviewLike = { callbacks.onLike(it) },
            onSeek = LocalVideoPageState.current.seekAction?.let { action ->
                { seconds: Int -> action(seconds * 1000L) }
            },
        ),
    )
    }
}
