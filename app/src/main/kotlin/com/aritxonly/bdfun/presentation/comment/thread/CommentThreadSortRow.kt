package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 展开页的「共 N 条评论 / 按热度」。
 *
 * 直接复用主评论区的 `CommentSortRow`：那两行的字号、内边距、点击热区都只有一份实现，
 * 展开页才不会比主评论区多缩进或少缩进一截。
 *
 * 视频详情走平铺列表：主评论区在列表 4dp 之外还有一层 8dp 内层，展开页没有那一层，
 * 这里按同一个 8dp 补齐，否则「共 N 条 / 按热度」会比主评论区更贴屏幕边缘。
 */
@Composable
internal fun CommentThreadSortRow(
    content: ThreadSheetContent,
    onToggleSort: () -> Unit,
    flatList: Boolean = false,
) {
    Box(Modifier.padding(horizontal = if (flatList) 8.dp else 0.dp)) {
        CommentSortRow(
            total = content.comment.replyCount + content.extraCount,
            sortHot = content.sortHot,
            onSort = onToggleSort,
        )
    }
}
