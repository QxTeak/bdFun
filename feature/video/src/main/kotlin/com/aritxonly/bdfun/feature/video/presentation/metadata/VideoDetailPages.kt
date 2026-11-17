package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailPages(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, onPage: (VideoPage) -> Unit,
) {
    // 首屏还没拿到详情时先摆分 P 骨架，数据到了直接原位替换，不做上浮。
    if (detail == null || !metaReady) {
        item(key = "pages-skeleton") { VideoPagesSkeleton() }
        return
    }
    if (detail.pages.size > 1) {
        item(key = "pages-card") {
            // 分 P 不要整体背景：每个 P 自己一张白色圆角卡片。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                    .padding(vertical = 8.dp),
            ) {
                VideoPageStrip(
                    pages = detail.pages,
                    currentCid = currentCid,
                    onSelect = { onPage(it) },
                )
            }
        }
    }
}
