package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.FullscreenBottomControls(
    info: FullscreenPlayerInfo, playback: FullscreenPlaybackState, actions: FullscreenActions,
    onPanelChanged: (FullscreenPanel) -> Unit,
    appearance: FullscreenAppearance,
    portrait: Boolean = false,
) {
        val hasNext = hasNextPage(info)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(
                    start = if (portrait) 12.dp else 42.dp,
                    end = if (portrait) 12.dp else 28.dp,
                    // 竖屏全屏没有系统导航条也按有小白条的高度抬高一点。
                    bottom = if (portrait) 22.dp else 6.dp,
                )
                .height(42.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerIconButton(onClick = actions.onPlayPause, size = 40.dp) {
                if (playback.playing) {
                    PauseGlyph()
                } else {
                    PlayGlyph(appearance.icons.play)
                }
            }
            Spacer(Modifier.width(12.dp))
            PlayerIconButton(
                onClick = actions.onNext,
                size = 32.dp,
            ) {
                NextGlyph(appearance.icons.next, enabled = hasNext)
            }
            Spacer(Modifier.width(if (portrait) 4.dp else 12.dp))
            // 竖屏也放回底部，用更小的图标和间距挤一挤。
            val danmakuSize = if (portrait) 28.dp else 32.dp
            Box(
                modifier = Modifier
                    .size(danmakuSize)
                    .clip(CircleShape)
                    .clickable(interactionSource = null, indication = null) {
                        actions.onToggleDanmaku()
                    },
                contentAlignment = Alignment.Center,
            ) {
                DanmakuToggleGlyph(appearance.icons.danmakuOn, appearance.icons.danmakuOff, enabled = playback.danmakuEnabled)
            }
            Spacer(Modifier.width(if (portrait) 4.dp else 8.dp))
            PlayerIconButton(
                onClick = { onPanelChanged(FullscreenPanel.Danmaku) },
                size = danmakuSize,
            ) {
                SlidersGlyph(appearance.icons.danmakuSettings)
            }
            Spacer(Modifier.width(if (portrait) 4.dp else 12.dp))
            if (portrait) {
                // 竖屏发送框缩成圆形按钮。
                PlayerIconButton(onClick = actions.onSendDanmaku, size = 28.dp) {
                    AppGlyph(com.aritxonly.teak.common.icons.LucideIcons.Send, Color.White, size = 17.dp)
                }
            } else {
                FullscreenSendDanmakuAction(actions.onSendDanmaku)
            }
            Spacer(Modifier.width(if (portrait) 6.dp else 14.dp))
            FullscreenTrailingActions(info, playback, actions, onPanelChanged, appearance, portrait)
        }
}

/** 右侧字幕 / 选集 / 倍速 / 清晰度：竖屏用更小的间距，横屏保持原值。 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.FullscreenTrailingActions(
    info: FullscreenPlayerInfo, playback: FullscreenPlaybackState, actions: FullscreenActions,
    onPanelChanged: (FullscreenPanel) -> Unit, appearance: FullscreenAppearance, portrait: Boolean,
) {
    PlayerTextAction(
        accentColor = appearance.accentColor,
        text = if (info.selectedSubtitle != null) "字幕·开" else "字幕",
        active = info.selectedSubtitle != null,
        onClick = { onPanelChanged(FullscreenPanel.Subtitles) },
    )
    Spacer(Modifier.width(if (portrait) 8.dp else 18.dp))
    PlayerTextAction(
        accentColor = appearance.accentColor,
        text = "选集",
        muted = info.pages.size <= 1,
        onClick = actions.onEpisodes,
    )
    Spacer(Modifier.width(if (portrait) 8.dp else 18.dp))
    PlayerTextAction(
        accentColor = appearance.accentColor,
        text = if (kotlin.math.abs(playback.speed - 1f) < 0.001f) "倍速" else "${trimSpeed(playback.speed)}x",
        onClick = { onPanelChanged(FullscreenPanel.Speed) },
    )
    Spacer(Modifier.width(if (portrait) 6.dp else 16.dp))
    QualityAction(label = info.qualityLabel, onClick = { onPanelChanged(FullscreenPanel.Quality) })
    Spacer(Modifier.width(12.dp))
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.FullscreenSendDanmakuAction(onSend: () -> Unit) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(Capsule())
                    .background(Color.White.copy(alpha = 0.8f))
                    .clickable(interactionSource = null, indication = null) {
                        onSend()
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = "发个友善的弹幕见证当下",
                    color = Color.Black.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    maxLines = 1,
                )
            }

}
