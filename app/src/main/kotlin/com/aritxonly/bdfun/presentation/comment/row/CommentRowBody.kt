package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

@Composable
internal fun CommentRowBody(
    item: CommentItem, disliked: Boolean, liked: Boolean, like: Int,
    ui: CommentRowPresentationStateHolder, actions: CommentRowActions,
    menuState: TeakMorphMenuState,
) {
    Row {
        CommentRowAvatar(item)
        Spacer(Modifier.width(9.dp))
        Column(modifier = Modifier.weight(1f)) {
            CommentRowAuthor(item)
            Spacer(Modifier.height(4.dp))
            CommentRowMessage(item)
            Spacer(Modifier.height(6.dp))
            CommentRowFooter(item, disliked, liked, like, ui, actions, menuState)

            ReplyBlock(item, CommentPreviewActions(actions.onOpenReplies, actions.onReplyToReply,
                actions.onLongPressReply, actions.onPreviewLike))
        }
    }
}
