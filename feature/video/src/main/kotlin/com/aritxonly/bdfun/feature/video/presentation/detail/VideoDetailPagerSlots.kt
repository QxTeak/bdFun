package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.video.VideoDetail
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.model.video.VideoPage

class VideoMetadataVisuals(val appearance: VideoMetadataAppearance, val slots: VideoMetadataSlots)
class VideoDetailRenderActions(
    val metadata: VideoMetadataActions, val onPage: (VideoPage) -> Unit, val onRelated: (VideoItem) -> Unit,
)
class VideoDetailPagerSlots(
    val metadata: @Composable () -> VideoMetadataVisuals,
    val tail: VideoDetailTailSlots,
    val comments: @Composable (VideoCommentPane) -> Unit,
)
/** 评论实现留app，模块沿用原公式只传显式几何与定位请求。 */
data class VideoCommentPane(
    val detail: VideoDetail, val root: Long, val target: Long,
    val topInset: Dp, val trackBackProgress: Boolean, val threadPlayerBottom: Dp,
    val threadPaneWidth: Dp?, val fullscreen: Boolean,
    /** 竖屏主评论区：标题上下留白收紧；横屏分栏（threadPaneWidth != null）保持原样。 */
    val compactHeaderSpacing: Boolean = false,
)
