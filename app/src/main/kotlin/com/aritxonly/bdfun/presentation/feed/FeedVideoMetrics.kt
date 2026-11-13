package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.VolumeOff
import top.yukonga.miuix.kmp.icon.extended.VolumeUp

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.FeedVideoMetrics(video: VideoItem, muted: Boolean, onToggleMute: () -> Unit) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .padding(start = 14.dp, end = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDuration(video.duration),
            color = Color.White,
            fontSize = 12.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.width(10.dp))
        MetricGlyph(play = true, size = 13.dp)
        Spacer(Modifier.width(3.dp))
        Text(
            text = formatCount(video.play),
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.width(10.dp))
        MetricGlyph(play = false, size = 13.dp)
        Spacer(Modifier.width(3.dp))
        Text(
            text = formatCount(video.danmaku),
            color = Color.White,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(30.dp)
                .clickable(interactionSource = null, indication = null) {
                    onToggleMute()
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (muted) MiuixIcons.VolumeOff else MiuixIcons.VolumeUp,
                contentDescription = if (muted) "静音" else "声音",
                tint = Color.White,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}
