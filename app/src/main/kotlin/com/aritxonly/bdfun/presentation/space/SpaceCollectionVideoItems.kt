package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.RelatedRow

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceCollectionVideoItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val collectionVideos = content.snapshot.collection.items
    val collectionLoading = content.snapshot.collection.loading
    val tabEnterModifier = Modifier
    // 每一条都作为独立的 Lazy 项，不再把几十条一起塞进一个 Column。
    // 这样滚动时只组合屏幕内可见的几行，投稿页不会一次卡住。
    items(
        items = collectionVideos,
        key = { video -> "collection-${video.bvid}" },
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
    if (collectionLoading) {
        item(key = "collection-loading") { SpaceLoadingRow() }
    } else if (collectionVideos.isEmpty()) {
        item(key = "collection-empty") { SpaceEmpty("合集里还没有视频") }
    }
}
