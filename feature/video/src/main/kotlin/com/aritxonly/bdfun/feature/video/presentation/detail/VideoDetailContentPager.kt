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
    content: VideoScreenContent, actions: VideoDetailRenderActions, slots: VideoDetailPagerSlots,
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
    val screenHeight = androidx.compose.ui.platform.LocalWindowInfo.current.containerDpSize.height
    VideoDetailPager(
        VideoDetailPaneState(fullscreen, pictureInPicture, splitPanes, paneLayout, playerBlockHeight, statusBarTop),
        pager, collapseConnection,
        metadata = { VideoScreenMetadata(content, pageUi, description, actions.metadata, slots) },
        tail = {
            val maxSheetHeight = (screenHeight - playerBlockHeight).coerceAtLeast(240.dp)
            videoScreenTail(content, metaReady, colors.onBackgroundVariant, actions, tailSlots, maxSheetHeight)
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
        VideoMetadataContent(detail, video, metaReady, upFollowers, upVideos, onlineCount, onlineText,
            content.metadata.tags),
        interactionState, description,
        actions,
        appearance = metadata.appearance, slots = metadata.slots,
    )
}

private fun androidx.compose.foundation.lazy.LazyListScope.videoScreenTail(
    content: VideoScreenContent, metaReady: Boolean,
    textColor: Color, actions: VideoDetailRenderActions, tailSlots: VideoDetailTailSlots,
    maxSheetHeight: androidx.compose.ui.unit.Dp,
) {
    val detail = content.read.detail
    val currentCid = content.selection.cid
    val error = content.read.error?.messageOr("加载失败")
    val related = content.read.related
    videoDetailTail(detail, metaReady, currentCid, error, related,
        textColor, actions.onPage,
        onRelated = actions.onRelated, slots = tailSlots, maxSheetHeight = maxSheetHeight)
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
    val initialCommentRoot = request.initialCommentRoot
    val initialCommentTarget = request.initialCommentTarget
    val d = detail
    if (d != null) {
        slots.comments(VideoCommentPane(
            detail = d, root = initialCommentRoot, target = initialCommentTarget,
            // 竖屏：评论列表自己会补一次状态栏，这里按「tab 行底边 + 12dp」反算，
            // 不再用固定 24+36：既贴合 tab，也不会在高状态栏机型上离得太远。
            // 横屏分栏：tab 行在详情栏顶部自带状态栏，只需要 52dp 行高。
            topInset = if (splitPanes) 52.dp else {
                val tabRowBottom = playerBlockHeight + 52.dp
                (tabRowBottom + 12.dp - statusBarTop - 4.dp).coerceAtLeast(0.dp)
            },
            trackBackProgress = pager.currentPage == 1,
            threadPlayerBottom = if (splitPanes) statusBarTop + 52.dp else playerBlockHeight,
            threadPaneWidth = if (splitPanes) paneWidth else null,
            fullscreen = fullscreen,
            compactHeaderSpacing = !splitPanes,
        ))
    }
}
