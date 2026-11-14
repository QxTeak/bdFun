package com.aritxonly.bdfun.ui


import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.VideoScreen
import kotlinx.coroutines.launch

@Composable
internal fun VideoActivityBody(
    isLoggedIn: () -> Boolean, settings: com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings,
    scene: VideoActivityScene, layout: VideoActivityLayout, ui: VideoActivityPageStateHolder,
    pager: androidx.compose.foundation.pager.PagerState, scope: kotlinx.coroutines.CoroutineScope,
    host: VideoActivityHostActions,
) {
    val VideoPageBus = LocalVideoPageState.current
    val PlayerUiState = VideoPageBus.playerUi
    val pipMode = layout.pictureInPicture
    val pageFullscreen = layout.fullscreen
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val statusBarDp = layout.statusBarTop
    val playerTopInset = layout.playerTopInset
    val loadedVideo = scene.video
    val commentRequest = scene.commentRequest
    var comments by ui.comments

    VideoPageBody(layout, ui, commentsVisible = pager.currentPage == 1, playerBottomPx = { VideoPageBus.playerBottomPx }, detail = {
        VideoScreen(
            isLoggedIn = isLoggedIn,
            request = com.aritxonly.bdfun.ui.VideoScreenRequest(
                video = loadedVideo,
                defaultQuality = settings.qualityPreference,
                openedFromVideoPage = host.openedFromVideoPage(),
                initialCommentRoot = commentRequest.first,
                initialCommentTarget = commentRequest.second,
                initialSeekMs = commentRequest.third,
            ),
            presentation = com.aritxonly.bdfun.ui.VideoScreenPresentation(
                paneLayout = paneLayout,
                pictureInPicture = pipMode,
                autoRotate = settings.autoRotate,
                immersiveStatusBar = settings.videoImmersiveStatusBar,
                // 从 Activity 根部读到的状态栏高度，避免页面内部读成 0。
                statusBarTop = statusBarDp,
            ),
            events = com.aritxonly.bdfun.ui.VideoScreenEvents(
                onOpenComments = {
                    if (paneLayout.isSplit) scope.launch { pager.animateScrollToPage(1) }
                    else comments = it
                },
                onBack = { host.onBack() },
            ),
            pager = pager,
        )
    }, comments = { c, onBack ->
        CommentScreen(
            request = CommentScreenRequest(aid = c.aid, upMid = c.ownerMid, title = c.title, bvid = c.bvid),
            onBack = onBack,
            layout = CommentScreenLayout(flatList = true),
        )
    })
}
