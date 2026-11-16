package com.aritxonly.bdfun.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.core.common.messageOr
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun androidx.compose.foundation.layout.BoxScope.VideoDetailContentPager(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    pageUi: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
    content: VideoScreenContent, actions: VideoDetailRenderActions, relatedEnter: Float, slots: VideoDetailPagerSlots,
) {
    val colors = MiuixTheme.colorScheme
    val fullscreen by pageUi.fullscreen
    val metaReady by pageUi.metaReady
    val pictureInPicture = presentation.pictureInPicture
    val paneLayout = presentation.paneLayout
    val splitPanes = paneLayout.isSplit && !fullscreen && !pictureInPicture
    val statusBarTop = presentation.statusBarTop
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val tailSlots = slots.tail
    val collapseConnection = inlineLayout.collapseConnection
    VideoDetailPager(
        VideoDetailPaneState(fullscreen, pictureInPicture, splitPanes, paneLayout, playerBlockHeight, statusBarTop),
        pager, collapseConnection,
        metadata = { VideoScreenMetadata(content, pageUi, description, actions.metadata, slots) },
        tail = {
            videoScreenTail(content, metaReady, relatedEnter, colors.onBackgroundVariant, actions, tailSlots)
        },
        comments = { paneWidth ->
            VideoScreenComments(request, presentation, pager, inlineLayout, pageUi, content, paneWidth, slots)
        },
    )
}

@Composable
private fun VideoScreenMetadata(
    content: VideoScreenContent, ui: VideoDetailUiStateHolder,
    description: VideoDescriptionStateHolder, actions: VideoMetadataActions, slots: VideoDetailPagerSlots,
) {
    val detail = content.read.detail
    val video = content.video
    val metaReady by ui.metaReady
    val upFollowers = content.metadata.followers
    val upVideos = content.metadata.videos
    val onlineCount = content.metadata.onlineCount
    val onlineText = content.metadata.onlineText
    val interactionState = content.interaction
    val metadata = slots.metadata()
    VideoMetadataSection(
        VideoMetadataContent(detail, video, metaReady, upFollowers, upVideos, onlineCount, onlineText),
        interactionState, description,
        actions,
        appearance = metadata.appearance, slots = metadata.slots,
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.videoScreenTail(
    content: VideoScreenContent, metaReady: Boolean, relatedEnter: Float,
    textColor: Color, actions: VideoDetailRenderActions, tailSlots: VideoDetailTailSlots,
) {
    val detail = content.read.detail
    val currentCid = content.selection.cid
    val error = content.read.error?.messageOr("加载失败")
    val related = content.read.related
    videoDetailTail(detail, metaReady, currentCid, error, related, relatedEnter,
        textColor, actions.onPage,
        onRelated = actions.onRelated, slots = tailSlots)
}

@Composable
private fun VideoScreenComments(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    ui: VideoDetailUiStateHolder, content: VideoScreenContent, paneWidth: Dp, slots: VideoDetailPagerSlots,
) {
    val detail = content.read.detail
    val fullscreen by ui.fullscreen
    val splitPanes = presentation.paneLayout.isSplit && !fullscreen && !presentation.pictureInPicture
    val statusBarTop = presentation.statusBarTop
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val immersiveStatusBar = presentation.immersiveStatusBar
    val initialCommentRoot = request.initialCommentRoot
    val initialCommentTarget = request.initialCommentTarget
    val d = detail
    if (d != null) {
        slots.comments(VideoCommentPane(
            detail = d, root = initialCommentRoot, target = initialCommentTarget,
            // 分栏时列表自己会补状态栏；这里只需要 tab 行高度（52dp），
            // 再留 4dp 给「共 N 条评论」，不能再重复加一次状态栏。
            topInset = if (splitPanes) 52.dp else playerBlockHeight + 24.dp +
                (if (immersiveStatusBar) 36.dp else 0.dp),
            trackBackProgress = pager.currentPage == 1,
            threadPlayerBottom = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight,
            threadPaneWidth = if (splitPanes) paneWidth else null,
            fullscreen = fullscreen,
        ))
    }
}
