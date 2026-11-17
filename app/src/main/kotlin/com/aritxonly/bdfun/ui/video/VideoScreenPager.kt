package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.VideoScreenPager(
    request: VideoScreenRequest, presentation: VideoScreenPresentation,
    pager: androidx.compose.foundation.pager.PagerState, inlineLayout: VideoInlineLayout,
    pageUi: VideoDetailUiStateHolder, description: VideoDescriptionStateHolder,
    content: VideoScreenContent, actions: VideoDetailViewActions, relatedEnter: Float,
) {
    val page = LocalVideoPageState.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val renderActions = VideoDetailRenderActions(
        VideoMetadataActions(actions::toggleFollow, actions::toggleLike, actions::toggleDislike,
            actions::giveCoin, actions::toggleFavorite, actions::shareVideo, actions::tripleAction,
            onOpenSpace = {
                page.pauseAll()
                navigateToSpace(context, content.read.detail?.ownerMid ?: content.video.ownerMid)
            }),
        onPage = actions::switchPage,
        onRelated = { page.pauseAll(); navigateToVideo(context, it, fromVideoPage = true) },
    )
    VideoDetailContentPager(request, presentation, pager, inlineLayout, pageUi, description,
        content, renderActions, relatedEnter, VideoDetailPagerSlots(
            metadata = {
                val metadata = rememberVideoMetadataAssembly()
                VideoMetadataVisuals(metadata.appearance, metadata.slots)
            },
            tail = rememberVideoDetailTailSlots(), comments = { pane ->
                VideoScreenComments(pane, scrollTop = pager.currentPage == 1)
            },
        ))
}

@Composable
private fun VideoScreenComments(pane: VideoCommentPane, scrollTop: Boolean) {
    val d = pane.detail
    // 换集后 bvid/aid 都会变；显式换 key，评论列表、滚动位置、楼中楼全部从新集重建。
    key(d.bvid, d.aid) {
        CommentScreen(
            request = CommentScreenRequest(aid = d.aid, upMid = d.ownerMid, title = d.title, bvid = d.bvid,
                focusRootRpid = pane.root, focusTargetRpid = pane.target),
            onBack = {},
            layout = CommentScreenLayout(bare = true, flatList = true, topInset = pane.topInset,
                trackBackProgress = pane.trackBackProgress, threadPlayerBottom = pane.threadPlayerBottom,
                threadPaneWidth = pane.threadPaneWidth, playerFullscreen = pane.fullscreen,
                scrollTop = scrollTop, compactHeaderSpacing = pane.compactHeaderSpacing),
        )
    }
}
