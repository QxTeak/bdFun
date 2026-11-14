package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.SponsorSegment
import com.kyant.shapes.Capsule

@Composable
fun BoxScope.PlayerAlwaysProgress(
    ui: VideoPlayerUiStateHolder, fullscreen: Boolean, positionMs: Long, durationMs: Long,
    progressBarMode: Int, accentColor: Color,
) {
    val controlsVisible by ui.controlsVisible
    val fsAudioOnly by ui.fsAudioOnly
    // 进度条常显：控件整体隐藏时，在画面最底部留一条 1dp 的粉色进度。
    val alwaysProgress = !controlsVisible && !fsAudioOnly &&
        (progressBarMode == 3 ||
            (progressBarMode == 1 && !fullscreen) ||
            (progressBarMode == 2 && fullscreen))
    if (alwaysProgress && durationMs > 0L) {
        val fraction = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.White.copy(alpha = 0.12f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(fraction)
                .height(2.dp)
                .background(accentColor),
        )
    }

}

@Composable
fun BoxScope.PlayerHotDanmakuOverlay(ui: VideoPlayerUiStateHolder, fullscreen: Boolean, hotDanmakuEnabled: Boolean) {
    val hotDanmaku by ui.hotDanmaku
    val controlsVisible by ui.controlsVisible
    val fsAudioOnly by ui.fsAudioOnly
    // 弹幕计数：按 BiliPai 的显示样式，纯白文字 + 黑色描影，居中等距排开，不用黑色胶囊。
    if (hotDanmakuEnabled &&
        hotDanmaku.isNotEmpty() && !controlsVisible && !fsAudioOnly
    ) {
        val hotShadow = androidx.compose.ui.text.TextStyle(
            shadow = androidx.compose.ui.graphics.Shadow(
                color = Color.Black,
                offset = androidx.compose.ui.geometry.Offset(1f, 1f),
                blurRadius = 3f,
            ),
        )
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                // 计数贴到画面最顶部（控件隐藏时这里本来就是空的），
                // 下面的弹幕行会按 topGuard 让开这块区域。
                .padding(top = if (fullscreen) 10.dp else 4.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            hotDanmaku.forEach { (text, count) ->
                Row(
                    modifier = Modifier.padding(1.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                        style = hotShadow,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "×$count",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        maxLines = 1,
                        style = hotShadow,
                    )
                }
            }
        }
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
