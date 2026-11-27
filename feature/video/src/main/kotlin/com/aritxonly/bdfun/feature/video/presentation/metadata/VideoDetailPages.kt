package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailPages(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, onPage: (VideoPage) -> Unit,
    slots: VideoDetailTailSlots,
    maxSheetHeight: androidx.compose.ui.unit.Dp,
) {
    // 首屏还没拿到详情：整页只保留元信息区那一个加载图标，这里不重复占位。
    if (detail == null || !metaReady) {
        return
    }
    if (detail.pages.size > 1) {
        item(key = "pages-card") {
            // 左上“分P” + 右上箭头；点开整页 sheet 选集。
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    // 分P 区紧贴上面的互动按钮，下面也少留白，避免整块显得松散。
                    .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
            ) {
                VideoPagesSection(
                    detail = detail, currentCid = currentCid, onPage = onPage,
                    cover = slots.cover, maxBodyHeight = maxSheetHeight,
                )
            }
        }
    }
}
