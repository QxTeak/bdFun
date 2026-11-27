package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoPage

fun androidx.compose.foundation.lazy.LazyListScope.videoDetailTail(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, error: String?, related: List<VideoItem>,
    textColor: Color,
    onPage: (VideoPage) -> Unit, onRelated: (VideoItem) -> Unit, slots: VideoDetailTailSlots,
    maxSheetHeight: androidx.compose.ui.unit.Dp,
) {
    videoDetailPages(detail, metaReady, currentCid, onPage, slots, maxSheetHeight)
    videoDetailCollection(detail, metaReady, currentCid, onPage, slots)
    if (error != null) {
        item(key = "error") {
            Text(
                text = error,
                modifier = Modifier.padding(16.dp),
                color = textColor,
                fontSize = 13.sp,
            )
        }
    }

    videoDetailRelated(related, metaReady, onRelated, slots)
}
