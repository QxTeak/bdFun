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
    layout: VideoActivityLayout, ui: VideoActivityPageStateHolder, commentsVisible: Boolean, playerBottomPx: () -> Float,
    detail: @Composable () -> Unit, comments: @Composable (VideoDetail, () -> Unit) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().background(LocalTeakContentColors.current.background)) {
        detail()
        VideoPageTopFade(layout, playerBottomPx)
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

@Composable
private fun androidx.compose.foundation.layout.BoxScope.VideoPageTopFade(layout: VideoActivityLayout, playerBottomPx: () -> Float) {
    val pipMode = layout.pictureInPicture
    val pageOverlayAlpha = layout.overlayAlpha
    val paneLayout = layout.paneLayout
    val splitPanes = layout.splitPanes
    val playerTopInset = layout.playerTopInset
    // 视频底边的白色渐变放进页面录制层，Tab 玻璃采样时会把它和下方内容
    // 当成同一张底片一起模糊，避免 Tab 内外一亮一白。
    val fadeColor = LocalTeakContentColors.current.background
    if (!splitPanes) Box(
        modifier = Modifier
            .align(if (splitPanes) Alignment.TopEnd else Alignment.TopStart)
            .then(if (splitPanes) Modifier.width(paneLayout.detailsWidthDp.dp) else Modifier.fillMaxWidth())
            .height(54.dp)
            .graphicsLayer {
                alpha = if (pipMode) 0f else pageOverlayAlpha
                // 渐变紧贴播放器真实底边。
                translationY = if (splitPanes) playerTopInset.toPx() else playerBottomPx()
            }
            .background(
                Brush.verticalGradient(
                    0f to fadeColor,
                    0.5f to fadeColor.copy(alpha = 0.94f),
                    0.8f to fadeColor.copy(alpha = 0.48f),
                    1f to fadeColor.copy(alpha = 0f),
                ),
            ),
    )
}
