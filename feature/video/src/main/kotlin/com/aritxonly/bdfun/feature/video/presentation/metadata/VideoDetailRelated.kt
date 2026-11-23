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
    // 首屏加载统一由元信息区的一个加载图标承担，这里不再重复出现。
    // 相关推荐可能来自上次退出时的读取缓存：详情还没就绪时不能抢先显示，
    // 否则用户看到的是「其他部分在加载、相关推荐已经出现」。
    if (!metaReady) return
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
