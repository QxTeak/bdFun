package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun FullscreenAudioMode(
    info: FullscreenPlayerInfo,
    playback: FullscreenPlaybackState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    hasPrevious: Boolean,
    hasNext: Boolean,
    modifier: Modifier = Modifier,
    appearance: FullscreenAppearance,
    slots: FullscreenChromeSlots,
) {

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val actions = FullscreenAudioActions(
            onBack, onPlayPause, onSeek, onPrevious, onNext, hasPrevious, hasNext,
        )
        FullscreenAudioBackground(info.cover, slots.cover)
        FullscreenAudioBody(info, playback, actions, appearance, slots)
        FullscreenAudioTopBar(info.title, onBack)
        FullscreenAudioStatus(appearance.showTime)
    }
}

private class FullscreenAudioActions(
    val onBack: () -> Unit,
    val onPlayPause: () -> Unit,
    val onSeek: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

@Composable
private fun FullscreenAudioBackground(cover: String, image: @Composable (String, Modifier) -> Unit) {
    // 不透明底：进听视频后下方 PlayerView 的视频帧不会透出来
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF08090D)))
    image(
        cover,
        Modifier
            .fillMaxSize()
            .blur(46.dp)
            .graphicsLayer { alpha = 0.55f },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Black.copy(alpha = 0.9f),
                ),
            ),
    )
}

@Composable
private fun FullscreenAudioBody(info: FullscreenPlayerInfo, playback: FullscreenPlaybackState, actions: FullscreenAudioActions, appearance: FullscreenAppearance, slots: FullscreenChromeSlots) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 76.dp, vertical = 40.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        slots.cover(
            info.cover,
            Modifier
                .size(360.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(70.dp))
        Column(modifier = Modifier.weight(1f)) {
            FullscreenAudioMetadata(info)
            FullscreenAudioTimeline(playback, actions.onSeek)
            FullscreenAudioTransport(playback, actions, appearance)
        }
    }
}

@Composable
private fun FullscreenAudioMetadata(info: FullscreenPlayerInfo) {
    Text(
        text = info.title,
        color = Color.White,
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(9.dp))
    Text(
        text = info.author.ifBlank { "UP 主" },
        color = Color.White.copy(alpha = 0.65f),
        fontSize = 14.sp,
        maxLines = 1,
    )
    Spacer(Modifier.height(34.dp))
}

@Composable
private fun FullscreenAudioTimeline(playback: FullscreenPlaybackState, onSeek: (Long) -> Unit) {
    FullscreenSeekBar(
        positionMs = playback.positionMs,
        durationMs = playback.durationMs,
        bufferedMs = playback.bufferedMs,
        onInteract = {},
        onSeek = onSeek,
        modifier = Modifier.fillMaxWidth(),
        externalPressed = playback.scrubbing,
        externalLook = playback.seekLook,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlayTime(playback.positionMs),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = formatPlayTime(playback.durationMs),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun FullscreenAudioTransport(
    playback: FullscreenPlaybackState,
    actions: FullscreenAudioActions,
    appearance: FullscreenAppearance,
) {
    Spacer(Modifier.height(24.dp))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        AudioStepButton(
            iconRes = appearance.icons.next,
            description = "上一集",
            mirror = true,
            enabled = actions.hasPrevious,
            onClick = actions.onPrevious,
        )
        Box(
            modifier = Modifier
                .size(58.dp)
                .clickable(interactionSource = null, indication = null, onClick = actions.onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            // 不再用白色圆底包住播放/暂停图标。
            if (playback.playing) PauseGlyph(Color.White) else PlayGlyph(appearance.icons.play, Color.White, 30.dp)
        }
        AudioStepButton(
            iconRes = appearance.icons.next,
            description = "下一集",
            mirror = false,
            enabled = actions.hasNext,
            onClick = actions.onNext,
        )
    }
}

/** 上一集 / 下一集：没有相邻分 P 时变暗且不可点。 */
@Composable
private fun AudioStepButton(
    iconRes: Int,
    description: String,
    mirror: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.13f else 0.06f))
            .clickable(interactionSource = null, indication = null, enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        NextGlyph(iconRes, enabled = enabled, mirror = mirror, description = description)
    }
}

@Composable
private fun BoxScope.FullscreenAudioTopBar(title: String, onBack: () -> Unit) {
    // 退出听视频固定到左上角，使用 Miuix 自带的关闭图标；标题跟在右边。
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .fillMaxWidth()
            .padding(start = 28.dp, top = 18.dp, end = 42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clickable(interactionSource = null, indication = null) {
                    onBack()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MiuixIcons.Close,
                contentDescription = "退出听视频",
                tint = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BoxScope.FullscreenAudioStatus(showTime: Boolean) {
    if (showTime) {
        FullscreenStatusLine(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 42.dp, vertical = 3.dp),
        )
    }
}
