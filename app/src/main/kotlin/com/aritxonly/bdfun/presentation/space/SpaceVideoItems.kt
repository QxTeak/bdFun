package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.RelatedRow

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceVideoItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val allVideos = content.snapshot.videos.items
    val videoLoading = content.snapshot.videos.loading
    val tabEnterModifier = Modifier
    items(
        items = allVideos,
        key = { video -> "video-${video.bvid}" },
    ) { video ->
        SpaceSubmissionVideoRow(modifier = tabEnterModifier) {
            RelatedRow(
                item = video,
                metaLabel = com.aritxonly.bdfun.ui.formatDate(video.pubdate),
                onClick = {
                    navigateToVideo(context, video)
                },
                horizontalPadding = 0.dp,
            )
        }
    }
    if (videoLoading) {
        item(key = "video-loading") { SpaceLoadingRow() }
    } else if (allVideos.isEmpty()) {
        item(key = "video-empty") { SpaceEmpty("还没有投稿") }
    }
}
