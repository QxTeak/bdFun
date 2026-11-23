package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 半屏听视频的选集动作：上一集 / 下一集以及是否可点。 */
class HalfAudioEpisodes(
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val hasPrevious: Boolean,
    val hasNext: Boolean,
)

@Composable
fun HalfScreenAudioMode(
    title: String,
    author: String,
    cover: String,
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onExit: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    episodes: HalfAudioEpisodes,
    appearance: InlinePlayerAppearance, coverImage: @Composable (String, Modifier) -> Unit,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090D)),
    ) {
        HalfAudioBackground(cover, coverImage)
        HalfAudioMetadata(title, author, cover, coverImage)
        HalfAudioExit(onExit)
        HalfAudioControls(playing, positionMs, durationMs, onPlayPause, onSeek, episodes, appearance)
    }
}



@Composable
private fun HalfAudioBackground(cover: String, coverImage: @Composable (String, Modifier) -> Unit) {
    coverImage(cover, Modifier
            .fillMaxSize()
            .blur(36.dp)
            .graphicsLayer { alpha = 0.42f },
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.2f),
                    1f to Color.Black.copy(alpha = 0.86f),
                ),
            ),
    )
}

/**
 * 封面按完整比例居中放在第一行，下面是居中的标题和 UP 名。
 * 不再铺「听视频」小字，也不再让封面和文字左右并排。
 */
@Composable
private fun BoxScope.HalfAudioMetadata(
    title: String,
    author: String,
    cover: String,
    coverImage: @Composable (String, Modifier) -> Unit,
) {
    Column(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .padding(start = 56.dp, top = 10.dp, end = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        coverImage(
            cover,
            Modifier
                .width(132.dp)
                .height(74.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = author.ifBlank { "UP 主" },
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}

/** 退出听视频：使用 Miuix 自带的关闭图标，固定在播放器左上角。 */
@Composable
private fun BoxScope.HalfAudioExit(onExit: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(top = 8.dp, start = 10.dp)
            .size(36.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onExit),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Close,
            contentDescription = "退出听视频",
            tint = Color.White.copy(alpha = 0.85f),
            modifier = Modifier.size(19.dp),
        )
    }
}

@Composable
private fun BoxScope.HalfAudioControls(
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    episodes: HalfAudioEpisodes,
    appearance: InlinePlayerAppearance,
) {
    Column(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 10.dp),
    ) {
        // 与视频播放器同款可拖动进度条，不再是纯展示的细线。
        FullscreenSeekBar(
            positionMs = positionMs,
            durationMs = durationMs,
            onInteract = {},
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = formatPlayTime(positionMs),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.weight(1f))
            HalfAudioStep(appearance.nextIcon, "上一集", episodes.hasPrevious, mirror = true, onClick = episodes.onPrevious)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clickable(interactionSource = null, indication = null, onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                // 暂停/播放只保留图标本身，不再套一圈圆形底。
                if (playing) PauseGlyph(Color.White) else PlayGlyph(appearance.playIcon, Color.White)
            }
            Spacer(Modifier.width(6.dp))
            HalfAudioStep(appearance.nextIcon, "下一集", episodes.hasNext, mirror = false, onClick = episodes.onNext)
            Spacer(Modifier.weight(1f))
            Text(
                text = formatPlayTime(durationMs),
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun HalfAudioStep(
    iconRes: Int,
    description: String,
    enabled: Boolean,
    mirror: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, enabled = enabled, onClick = onClick)
            .padding(8.dp),
    ) {
        NextGlyph(iconRes, enabled = enabled, mirror = mirror, description = description)
    }
}
