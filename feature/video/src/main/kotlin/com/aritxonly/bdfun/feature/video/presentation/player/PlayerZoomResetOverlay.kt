package com.aritxonly.bdfun.ui




import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun BoxScope.PlayerZoomResetOverlay(ui: VideoPlayerUiStateHolder, fullscreen: Boolean) {
    val zoomScope = rememberCoroutineScope()
    var videoScale by ui.videoScale
    var videoRotation by ui.videoRotation
    var videoOffset by ui.videoOffset
    // 跟随播放控件一起显隐：控件收起时提示也收起，不常驻遮挡画面。
    val controlsVisible by ui.controlsVisible
    // 全屏：完整复刻官方横屏 UI（顶部状态/互动，底部控制）
    if (controlsVisible && (videoScale > 1.01f || videoScale < 0.99f || kotlin.math.abs(videoRotation) > 0.5f)) {
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = if (fullscreen) 94.dp else 58.dp)
                .clip(Capsule())
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(interactionSource = null, indication = null) {
                    val fromScale = videoScale
                    val fromRotation = videoRotation
                    val fromOffset = videoOffset
                    zoomScope.launch {
                        val start = System.nanoTime()
                        while (true) {
                            val t = ((System.nanoTime() - start) / 1_000_000f / 260f)
                                .coerceIn(0f, 1f)
                            val e = 1f - (1f - t) * (1f - t) * (1f - t)
                            videoScale = fromScale + (1f - fromScale) * e
                            videoRotation = fromRotation * (1f - e)
                            videoOffset = fromOffset * (1f - e)
                            if (t >= 1f) break
                            withFrameNanos { }
                        }
                        videoScale = 1f
                        videoRotation = 0f
                        videoOffset = Offset.Zero
                    }
                }
                .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
            Text(
                text = "取消缩放",
                color = Color(0xFFFB7299),
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }

}
