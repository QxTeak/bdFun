package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.LocalTeakContentColors
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoDetail

@Composable
fun VideoPageBody(
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder, commentsVisible: Boolean,
    detail: @Composable () -> Unit, comments: @Composable (VideoDetail, () -> Unit) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(LocalTeakContentColors.current.background)) {
        detail()
        if (commentsVisible) VideoCommentBottomFade(layout)
        var current by ui.comments
        val c = if (layout.pictureInPicture) null else current
        if (c != null) {
            Box(modifier = Modifier.fillMaxSize().background(LocalTeakContentColors.current.background)) {
                comments(c) { current = null }
            }
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoCommentBottomFade(layout: VideoActivityLayout) {
    val pipMode = layout.pictureInPicture
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    // 页面底部反向渐变：顶部透明，越往下越接近页面底色。
    Box(
        modifier = Modifier
            .align(if (splitPanes) Alignment.BottomEnd else Alignment.BottomCenter)
            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
            .height(54.dp)
            .graphicsLayer { alpha = if (pipMode) 0f else pageOverlayAlpha }
            .background(
                Brush.verticalGradient(
                    listOf(
                        LocalTeakContentColors.current.background.copy(alpha = 0f),
                        LocalTeakContentColors.current.background,
                    ),
                ),
            ),
    )
}
