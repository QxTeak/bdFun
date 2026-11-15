package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp

@Composable
internal fun CommentThreadList(render: CommentThreadRender) {
    val content = render.content
    val allReplies = content.allReplies
    val end = content.end
    val loading = content.loading
    val transient = render.transient
    val callbacks = render.callbacks
    val listState = render.listState
    val playerCollapseConnection = render.connection
    val headerHeight = 46.dp
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(playerCollapseConnection),
        contentPadding = PaddingValues(
            // 左右留白必须和主评论区一致：平铺列表（视频详情）是 4dp，其它入口 12dp。
            start = if (render.layout.flatList) 4.dp else 12.dp,
            end = if (render.layout.flatList) 4.dp else 12.dp,
            top = headerHeight + 8.dp,
            bottom = if (transient.replyTarget != null) 78.dp else 16.dp,
        ),
    ) {
        item(key = "thread-op") {
            CommentThreadParentCard(content.comment, transient, callbacks)
        }

        item(key = "thread-head") {
            CommentThreadSortRow(content, callbacks.onToggleSort)
        }

        itemsIndexed(allReplies, key = { _, r -> r.rpid }) { index, reply ->
            CommentThreadReplyCard(reply, index, allReplies.lastIndex, end, transient, callbacks)
        }

        if (loading) {
            item(key = "thread-loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    TeakLoading(size = 34.dp)
                }
            }
        }
    }
}
