package com.aritxonly.bdfun.ui

import androidx.compose.runtime.mutableStateOf
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

internal data class CommentRowActions(
    val onLike: () -> Unit, val onDislike: () -> Unit, val onReply: () -> Unit,
    val onLongPress: (androidx.compose.ui.geometry.Rect, TeakMorphMenuState) -> Unit,
    val onMore: (androidx.compose.ui.geometry.Rect, TeakMorphMenuState) -> Unit,
    val onOpenReplies: () -> Unit, val onReplyToReply: (CommentItem) -> Unit,
    val onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect, TeakMorphMenuState) -> Unit,
    val onPreviewLike: (CommentItem) -> Unit,
)
internal data class CommentPreviewActions(
    val onOpen: () -> Unit, val onReplyToReply: (CommentItem) -> Unit,
    val onLongPressReply: (CommentItem, androidx.compose.ui.geometry.Rect, TeakMorphMenuState) -> Unit,
    val onPreviewLike: (CommentItem) -> Unit,
)
internal class CommentRowPresentationStateHolder {
    /** 只在长按事件里读取；滚动时每帧写 State 会让整行反复失效，这里用普通字段。 */
    var bounds: androidx.compose.ui.geometry.Rect = androidx.compose.ui.geometry.Rect.Zero
}
