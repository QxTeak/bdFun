package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun VideoDetailLayout(
    presentation: VideoScreenPresentation, inlineLayout: VideoInlineLayout,
    pageUi: VideoDetailUiStateHolder, loading: Boolean, slots: VideoDetailLayoutSlots,
) {
    val fullscreen by pageUi.fullscreen
    val playing by pageUi.playing
    val colors = MiuixTheme.colorScheme
    val pictureInPicture = presentation.pictureInPicture
    val paneLayout = presentation.paneLayout
    val immersiveStatusBar = presentation.immersiveStatusBar
    val statusBarTop = presentation.statusBarTop
    val statusTop = statusBarTop
    val splitPanes = paneLayout.isSplit && !fullscreen && !pictureInPicture
    val playerBlockHeight = inlineLayout.playerBlockHeight
    val expandedPlayerHeight = inlineLayout.expandedPlayerHeight
    val collapsedPlayerHeight = inlineLayout.collapsedPlayerHeight
    val animatedPlayerHeight = inlineLayout.animatedPlayerHeight
    val collapseState = inlineLayout.collapseState
    Box(modifier = Modifier.fillMaxSize()) {
        slots.pager(this)

        if (loading) {
            slots.loading(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp)
                    .size(28.dp),
                colors.primary,
            )
        }

        val viewport = resolveVideoPlayerViewport(VideoViewportInput(
            expandedPlayerHeight, collapsedPlayerHeight, animatedPlayerHeight, statusTop, immersiveStatusBar,
            fullscreen, pictureInPicture, splitPanes,
        ))
        slots.player(this, viewport)

        VideoCollapseOverlay(
            fullscreen = fullscreen,
            collapseProgress = viewport.collapseProgress,
            playerBlockHeight = playerBlockHeight,
            statusTop = statusTop,
            accentColor = slots.accentColor,
            playIcon = slots.playIcon,
            onRestorePlay = {
                collapseState.restore()
                if (!playing) slots.togglePlayback()
            },
            onBack = slots.onBack,
        )
    }

}
