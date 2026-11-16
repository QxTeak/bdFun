package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule

@Composable
internal fun GestureIconTip(
    brightness: Boolean,
    value: Float,
    modifier: Modifier = Modifier,
) {

    Row(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppGlyph(if (brightness) LucideIcons.Sun else LucideIcons.Volume1, Color.White, size = 24.dp)
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(100.dp)
                .height(4.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.25f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value.coerceIn(0f, 1f))
                    .height(4.dp)
                    .clip(Capsule())
                    .background(Color(0xFFFB7299)),
            )
        }
    }
}

@Composable
internal fun GestureTip(label: String, modifier: Modifier = Modifier) {

    Box(
        modifier = modifier
            .clip(Capsule())
            .background(Color.Black.copy(alpha = 0.50f))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
    }
}

fun togglePlay(playback: PlaybackController, playing: Boolean) {
    if (playing) playback.pause() else playback.play()
}
