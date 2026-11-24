package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoPage

/** 前后台共用选集顺序：多 P 优先，接合集其余单集，按 cid 去重。 */
fun videoPlaybackPages(detail: VideoDetail): List<VideoPage> {
    val episodes = detail.episodes.mapIndexed { index, episode ->
        VideoPage(episode.cid, index + 1, episode.title, episode.duration)
    }
    if (detail.pages.size > 1) return (detail.pages + episodes).distinctBy { it.cid }
    if (episodes.size > 1) return episodes
    return detail.pages
}

/**
 * 合集第一集在 [videoPlaybackPages] 结果里的下标，没有合集时返回 -1。
 *
 * 列表循环放到合集最后一集后要回到这里重新循环，所以起点不能靠分 P 数量去猜：
 * 单 P 视频的结果是纯合集列表，合集从第 0 项开始。
 */
fun collectionStartIndex(detail: VideoDetail): Int {
    if (detail.episodes.size <= 1) return -1
    if (detail.pages.size <= 1) return 0
    return videoPlaybackPages(detail).indexOfFirst { page ->
        detail.pages.none { it.cid == page.cid }
    }
}