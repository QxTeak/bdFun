package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.teak.common.theme.LocalTeakContentColors

@Composable
internal fun ReplyBlock(item: CommentItem, actions: CommentPreviewActions) {
    if (item.replyCount <= 0) return
    val replyBackground = LocalTeakContentColors.current.groupedBackground
    val preview = item.previewReplies.take(2)
    Spacer(Modifier.height(8.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                    Modifier
                } else {
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        // 与详情合集共用分组底色，避免透明叠色随父背景变化。
                        .background(replyBackground)
                },
            )
            .padding(
                horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 10.dp,
                vertical = 8.dp,
            ),
    ) {
        preview.forEachIndexed { replyIndex, reply ->
            CommentPreviewRow(reply, actions)
            // 折叠回复之间多留 2dp，避免两条挤在一起。
            if (replyIndex != preview.lastIndex) {
                Spacer(Modifier.height(2.dp))
            }
        }

        // 点这里打开大面板看全部回复
        CommentPreviewOpenLink(item, actions.onOpen)
    }
}
