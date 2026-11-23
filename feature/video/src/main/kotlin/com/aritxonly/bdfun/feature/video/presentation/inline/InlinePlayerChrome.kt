package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.ResourceGlyph

import com.aritxonly.teak.common.player.PlayGlyph
import com.aritxonly.teak.common.player.PauseGlyph

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

@Composable
fun InlineCompactChrome(state: InlineChromeState, actions: InlineChromeActions, appearance: InlinePlayerAppearance) {

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        InlineChromeMasks()
        InlineChromeTopActions(state, actions)
        InlineChromeTransport(state, actions, appearance)
    }
}

@Composable
private fun BoxScope.InlineChromeMasks() {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .height(80.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.65f),
                ),
            ),
    )

    // 顶部遮罩：和底部一样压一条黑色渐变，否则半屏时顶部控件直接压在画面上
    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .height(64.dp)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    0.55f to Color.Black.copy(alpha = 0.25f),
                    1f to Color.Transparent,
                ),
            ),
    )

}

@Composable
private fun BoxScope.InlineChromeTopActions(state: InlineChromeState, actions: InlineChromeActions) {
    Row(
        modifier = Modifier
            .align(Alignment.TopStart)
            .then(if (state.immersiveStatusBar) Modifier.statusBarsPadding() else Modifier)
            .graphicsLayer { alpha = if (state.scrubbing) 0f else 1f }
            .padding(start = 10.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        InlineTopIcon(dark = true, size = 34.dp, onClick = actions.onBack) {
            InlineBackGlyph()
        }
        InlineTopIcon(filled = true, size = 38.dp, onClick = actions.onHome) {
            InlineHomeGlyph(tint = Color.White)
        }
    }

    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .then(if (state.immersiveStatusBar) Modifier.statusBarsPadding() else Modifier)
            .graphicsLayer { alpha = if (state.scrubbing) 0f else 1f }
            .padding(end = 10.dp, top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        InlineTopIcon(size = 34.dp, onClick = actions.onListen) { InlineHeadphoneGlyph() }
        InlineTopIcon(size = 34.dp, onClick = actions.onCast) { InlineCastGlyph() }
        InlineTopIcon(filled = true, size = 38.dp, onClick = actions.onMore) {
            InlineMoreGlyph(tint = Color.White)
        }
    }

}

@Composable
private fun BoxScope.InlineChromeTransport(state: InlineChromeState, actions: InlineChromeActions, appearance: InlinePlayerAppearance) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 10.dp, end = 12.dp, bottom = 7.dp)
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompactIconButton(onClick = actions.onPlayPause, size = 28.dp) {
            if (state.playing) PauseGlyph() else PlayGlyph(appearance.playIcon, size = 22.dp)
        }
        Spacer(Modifier.width(7.dp))
        FullscreenSeekBar(
            positionMs = state.positionMs,
            durationMs = state.durationMs,
            bufferedMs = state.bufferedMs,
            onInteract = {},
            onSeek = actions.onSeek,
            modifier = Modifier.weight(1f),
            externalPressed = state.scrubbing,
            externalLook = state.seekLook,
            onScrubStart = actions.onScrubStart,
            onScrubEnd = actions.onScrubEnd,
            onLookChanged = actions.onScrubLook,
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = "${formatPlayTime(state.positionMs)}/${formatPlayTime(state.durationMs)}",
            color = Color.White,
            fontSize = 11.5.sp,
            maxLines = 1,
        )
        Spacer(Modifier.width(7.dp))
        CompactIconButton(onClick = actions.onFullscreen, size = 29.dp) {
            ResourceGlyph(res = appearance.fullscreenIcon, tint = Color.White, size = 17.dp)
        }
    }
}

@Composable
internal fun InlineTopIcon(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 34.dp,
    filled: Boolean = false,
    dark: Boolean = false,
    content: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(if (filled) 12.dp else 50.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
internal fun CompactIconButton(
    onClick: () -> Unit,
    size: androidx.compose.ui.unit.Dp = 36.dp,
    content: @Composable () -> Unit,
) {

    Box(
        modifier = Modifier
            .size(size)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * 长按倍速提示：仍然是原来那样三颗三角依次闪烁，只是三角改用播放/暂停按钮那颗
 * （同一份 drawable），每颗保持原来的 8dp 宽、11dp 高、3dp 间距。
 */
@Composable
fun BoostTriangles(playIcon: Int) {

    val transition = rememberInfiniteTransition(label = "boost-triangles")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { index ->
            val pulse = ((phase + index * 0.5f) % 2f).let { if (it > 1f) 2f - it else it }
            val alpha = 0.25f + 0.75f * pulse
            com.aritxonly.teak.common.player.PixelIcon(
                res = playIcon,
                modifier = Modifier.size(width = 8.dp, height = 11.dp),
                tint = Color.White.copy(alpha = alpha),
                contentDescription = null,
            )
        }
    }
}
