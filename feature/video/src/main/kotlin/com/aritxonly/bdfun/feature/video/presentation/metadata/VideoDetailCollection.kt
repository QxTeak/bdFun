package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

internal fun androidx.compose.foundation.lazy.LazyListScope.videoDetailCollection(
    detail: VideoDetail?, metaReady: Boolean, currentCid: Long, onPage: (VideoPage) -> Unit, slots: VideoDetailTailSlots,
) {
    // 首屏先摆合集骨架；数据到了直接换内容，不再整卡上浮。
    if (detail == null || !metaReady) {
        item(key = "collection-skeleton") { VideoCollectionSkeleton() }
        return
    }
    if (detail.episodes.size > 1) {
        item(key = "collection-card") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp),
            ) {
                VideoCollectionCard(
                    title = detail.seasonTitle.ifBlank { detail.title },
                    episodes = detail.episodes,
                    currentCid = currentCid,
                    slots = slots,
                    onSelect = { episode ->
                        val index = detail.episodes
                            .indexOf(episode)
                            .coerceAtLeast(0)
                        onPage(
                            com.aritxonly.bdfun.core.model.video.VideoPage(
                                cid = episode.cid,
                                page = index + 1,
                                part = episode.title,
                                duration = episode.duration,
                            ),
                        )
                    },
                )
            }
        }
    }
}
