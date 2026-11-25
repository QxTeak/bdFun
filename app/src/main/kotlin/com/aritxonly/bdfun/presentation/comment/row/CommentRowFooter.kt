package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

@Composable
internal fun CommentRowFooter(
    item: CommentItem, disliked: Boolean, liked: Boolean, like: Int,
    ui: CommentRowPresentationStateHolder, actions: CommentRowActions,
    menuState: TeakMorphMenuState,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentRowMetadata(item, actions.onReply)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        // 点赞：整体略上移
        CommentRowLike(liked, like, actions.onLike)
        Spacer(Modifier.width(10.dp))
        // 点踩：整体略下移
        CommentRowDislike(disliked, actions.onDislike)
        Spacer(Modifier.width(10.dp))
        // 省略号：和视频卡片同一套形变菜单，从图标位置展开。
        CommentRowMore(
            onMore = { actions.onMore(ui.bounds, menuState) },
            morphState = menuState,
        )
    }
}
