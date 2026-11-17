package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailRelated(
    related: List<VideoItem>, metaReady: Boolean, onRelated: (VideoItem) -> Unit, slots: VideoDetailTailSlots,
) {
    // 相关推荐没到手时先摆行骨架；到位后原位替换，不做整列上浮。
    if (related.isEmpty() && !metaReady) {
        item(key = "related-skeleton") { VideoRelatedSkeleton() }
        return
    }
    if (related.isEmpty()) return
    itemsIndexed(
        items = related,
        key = { _, item -> "related-${item.bvid}" },
    ) { index, relatedItem ->
        val isFirst = index == 0
        val isLast = index == related.lastIndex
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = if (isFirst) 14.dp else 0.dp,
                    bottom = if (isLast) 6.dp else 0.dp,
                )
                .padding(vertical = 2.dp),
        ) {
            slots.relatedRow(relatedItem) { onRelated(relatedItem) }
        }
    }
}
