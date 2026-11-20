package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.data.VideoItem

/** 视频页入参快照：commentRequest = (root, target, seek)。 */
internal data class VideoActivityScene(
    val video: VideoItem,
    val commentRequest: Triple<Long, Long, Long>,
    /** PGC 入口的 ep_id；普通稿件为 0。 */
    val epId: Long = 0L,
    val seasonId: Long = 0L,
)
