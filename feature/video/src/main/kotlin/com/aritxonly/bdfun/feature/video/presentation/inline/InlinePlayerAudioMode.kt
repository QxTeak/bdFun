package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
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
    episodes: HalfAudioEpisodes,
    appearance: InlinePlayerAppearance, coverImage: @Composable (String, Modifier) -> Unit,
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF08090D)),
    ) {
        HalfAudioBackground(cover, coverImage)
        HalfAudioMetadata(title, author, cover, appearance.accentColor, coverImage)
        HalfAudioExit(onExit)
        HalfAudioControls(playing, positionMs, durationMs, onPlayPause, episodes, appearance.playIcon)
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

@Composable
private fun BoxScope.HalfAudioMetadata(title: String, author: String, cover: String, accentColor: Color, coverImage: @Composable (String, Modifier) -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 14.dp, top = 12.dp, end = 88.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        coverImage(cover, Modifier
                .size(66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("听视频", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Normal)
            Spacer(Modifier.height(3.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = author.ifBlank { "UP 主" },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BoxScope.HalfAudioExit(onExit: () -> Unit) {
    Text(
        text = "退出听视频",
        color = Color.White.copy(alpha = 0.78f),
        fontSize = 11.sp,
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 16.dp, end = 14.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = null, indication = null, onClick = onExit)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    )
}

@Composable
private fun BoxScope.HalfAudioControls(
    playing: Boolean,
    positionMs: Long,
    durationMs: Long,
    onPlayPause: () -> Unit,
    episodes: HalfAudioEpisodes,
    playIcon: Int,
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatPlayTime(positionMs),
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
        )
        Spacer(Modifier.weight(1f))
        HalfAudioStep(MiuixIcons.ChevronBackward, "上一集", episodes.hasPrevious, episodes.onPrevious)
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable(interactionSource = null, indication = null, onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            if (playing) PauseGlyph() else PlayGlyph(playIcon)
        }
        Spacer(Modifier.width(6.dp))
        HalfAudioStep(MiuixIcons.ChevronForward, "下一集", episodes.hasNext, episodes.onNext)
        Spacer(Modifier.weight(1f))
        Text(
            text = formatPlayTime(durationMs),
            color = Color.White.copy(alpha = 0.62f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun HalfAudioStep(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, enabled = enabled, onClick = onClick)
            .padding(8.dp),
    ) {
        AppGlyph(
            icon = icon,
            tint = Color.White.copy(alpha = if (enabled) 0.85f else 0.28f),
            size = 18.dp,
            contentDescription = description,
        )
    }
}
