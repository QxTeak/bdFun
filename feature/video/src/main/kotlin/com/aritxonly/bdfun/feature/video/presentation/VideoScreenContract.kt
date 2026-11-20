package com.aritxonly.bdfun.ui

import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem

data class VideoScreenRequest(
    val video: VideoItem,
    val defaultQuality: Int = 80,
    val openedFromVideoPage: Boolean = false,
    val initialCommentRoot: Long = 0L,
    val initialCommentTarget: Long = 0L,
    val initialSeekMs: Long = 0L,
    /** PGC 入口：直接按 ep_id 取详情/播放源，评论也用 ep_id。普通稿件为 0。 */
    val epId: Long = 0L,
    val seasonId: Long = 0L,
)

data class VideoScreenPresentation(
    val paneLayout: VideoPaneLayout,
    val statusBarTop: Dp,
    val pictureInPicture: Boolean = false,
    val immersiveStatusBar: Boolean = false,
    val autoRotate: Boolean = true,
)

class VideoScreenEvents(
    val onOpenComments: (VideoDetail) -> Unit = {},
    val onBack: () -> Unit,
)
