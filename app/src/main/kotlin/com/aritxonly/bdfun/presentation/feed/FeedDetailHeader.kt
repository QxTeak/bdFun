package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.feed.application.FeedDetailUiState
import com.aritxonly.bdfun.ui.FeedCard
import com.aritxonly.bdfun.ui.FeedCardActions
import com.aritxonly.bdfun.ui.FeedCardPresentation
import com.aritxonly.teak.common.chrome.TeakLargeTitle

internal fun LazyListScope.dynamicDetailHeader(snapshot: FeedDetailUiState, actions: FeedDetailActions) {
    val detail = snapshot.detail
    val liked = snapshot.liked
    val likeCount = snapshot.likeCount
    item(key = "detail-big-title") { TeakLargeTitle(title = "动态详情") }
    item(key = "detail-dynamic") {
        Box(modifier = Modifier.padding(bottom = 4.dp)) {
            FeedCard(
                item = detail.copy(
                    counts = detail.counts.copy(
                        liked = liked,
                        like = likeCount,
                    ),
                ),
                presentation = FeedCardPresentation(liked, expanded = true, animatedOpen = false),
                actions = FeedCardActions(
                    onOpen = {},
                    onOpenVideo = actions::openVideo,
                    onOpenForwarded = actions::openForwarded,
                    onLike = actions::like,
                    onComment = {},
                    onRepost = {},
                ),
                flat = true,
            )
        }
    }
    // 评论标题/排序行由 CommentScreen 自己的「共 N 条评论」承载，页面不再单独写一套。
}
