package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.feed.application.FeedDetailUiState
import com.aritxonly.bdfun.ui.FeedCard
import com.aritxonly.bdfun.ui.FeedCardActions
import com.aritxonly.bdfun.ui.FeedCardPresentation
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

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
            )
        }
    }
    item(key = "detail-comment-label") {
        Text(
            text = "评论",
            color = MiuixTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 2.dp, top = 16.dp, bottom = 2.dp),
        )
    }
}
