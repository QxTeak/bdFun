package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.kyant.shapes.Capsule

@Composable
fun BoxScope.PlayerAlwaysProgress(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean,
    progressBarMode: Int, accentColor: Color,
) {
    val controlsVisible by ui.controlsVisible
    val fsAudioOnly by ui.fsAudioOnly
    // 进度条常显：控件整体隐藏时，在画面最底部留一条 1dp 的粉色进度。
    val alwaysProgress = !controlsVisible && !fsAudioOnly &&
        (progressBarMode == 3 ||
            (progressBarMode == 1 && !fullscreen) ||
            (progressBarMode == 2 && fullscreen))
    if (alwaysProgress) {
        // 位置/总长只在绘制阶段读取：每秒一次的值变化只重画这一条线，播放器整块不重组。
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .drawBehind {
                    val total = ui.progressDurationMs.longValue
                    if (total <= 0L) return@drawBehind
                    drawRect(Color.White.copy(alpha = 0.12f))
                    val fraction = (ui.progressPositionMs.longValue.toFloat() / total).coerceIn(0f, 1f)
                    drawRect(accentColor, size = Size(size.width * fraction, size.height))
                },
        )
    }

}

@Composable
fun BoxScope.PlayerSponsorOverlay(
    activeSponsor: SponsorSegment?, fullscreen: Boolean, accentColor: Color, onSkip: (SponsorSegment) -> Unit,
) {
    // 空降助手：进入赞助片段时在右下角出现跳过按钮。
    activeSponsor?.let { segment ->
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = if (fullscreen) 104.dp else 62.dp,
                )
                .clip(Capsule())
                .background(Color.Black.copy(alpha = 0.50f))
                .clickable(interactionSource = null, indication = null) {
                    onSkip(segment)
                }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "空降助手 · ${sponsorCategoryLabel(segment.category)}",
                color = Color.White,
                fontSize = 12.sp,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "跳过",
                color = accentColor,
                fontSize = 12.sp,
            )
        }
    }
}
