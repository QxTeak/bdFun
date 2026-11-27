package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule

@Composable
fun BoxScope.PlayerStatusOverlays(
    ui: VideoPlayerUiStateHolder,
    fullscreen: Boolean,
    ended: Boolean,
    durationMs: Long,
    playIcon: Int,
    buffering: @Composable (Modifier) -> Unit,
    onReplay: () -> Unit,
) {
    val showBuffering by ui.showBuffering
    // 超过 1.5s 仍缓冲才显示小电视；首帧前的封面由页面组装层显示。

    // 播放中加载：粉色矢量小电视 + 三点省略号 + 正在缓冲...
    if (showBuffering) {
        buffering(Modifier.align(Alignment.Center))
    }

    // 播放结束：整屏压暗，居中「播放完成」+ 胶囊重播键。点击胶囊从 0 重新播放，
    // 已缓冲区间仍由 ExoPlayer 保留，回退/重播不会重新走网络。
    if (ended) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.42f)),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Color(0x8C000000))
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                ) {
                    Text(
                        text = "播放完成",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .clip(Capsule())
                        .background(Color.White.copy(alpha = 0.92f))
                        .clickable(interactionSource = null, indication = null, onClick = onReplay)
                        .padding(horizontal = 18.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppGlyph(playIcon, Color(0xFF111111), size = 17.dp)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        text = "重新播放",
                        color = Color(0xFF111111),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }

    PlayerBoostOverlay(ui, fullscreen, playIcon)
    PlayerGestureTips(ui, durationMs)
}

@Composable
private fun BoxScope.PlayerBoostOverlay(ui: VideoPlayerUiStateHolder, fullscreen: Boolean, playIcon: Int) {
    val boosting by ui.boosting
    val boostSpeed by ui.boostSpeed
    if (boosting) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .then(if (fullscreen) Modifier.statusBarsPadding() else Modifier)
                .padding(top = 12.dp)
                .clip(Capsule())
                .background(Color.Black.copy(alpha = 0.50f))
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BoostTriangles(playIcon)
                Spacer(Modifier.width(8.dp))
                Text(
                    text = String.format(
                        java.util.Locale.US,
                        "%.1f× 倍速播放中",
                        boostSpeed,
                    ),
                    color = Color.White,
                    fontSize = 12.sp,
                )
            }
        }
    }

}

@Composable
private fun BoxScope.PlayerGestureTips(ui: VideoPlayerUiStateHolder, durationMs: Long) {
    val brightnessTip by ui.brightnessTip
    val volumeTip by ui.volumeTip
    val seekPreviewMs by ui.seekPreviewMs
    brightnessTip?.let { value ->
        GestureIconTip(
            brightness = true,
            value = value,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    volumeTip?.let { value ->
        GestureIconTip(
            brightness = false,
            value = value,
            modifier = Modifier.align(Alignment.Center),
        )
    }
    if (seekPreviewMs >= 0L) {
        GestureTip(
            label = "${formatPlayTime(seekPreviewMs)} / ${formatPlayTime(durationMs)}",
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
        )
    }

}
