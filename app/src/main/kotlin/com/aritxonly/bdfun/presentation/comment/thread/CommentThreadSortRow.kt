package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable

/**
 * 展开页的「共 N 条评论 / 按热度」。
 *
 * 直接复用主评论区的 `CommentSortRow`：那两行的字号、内边距、点击热区都只有一份实现，
 * 展开页才不会比主评论区多缩进或少缩进一截。
 */
@Composable
internal fun CommentThreadSortRow(content: ThreadSheetContent, onToggleSort: () -> Unit) {
    CommentSortRow(
        total = content.comment.replyCount + content.extraCount,
        sortHot = content.sortHot,
        onSort = onToggleSort,
    )
}
