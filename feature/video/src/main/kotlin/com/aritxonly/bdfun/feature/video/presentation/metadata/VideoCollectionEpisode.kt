package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.common.formatCount
import com.aritxonly.bdfun.core.common.formatDateTime
import com.aritxonly.bdfun.core.common.formatDuration
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoCollectionEpisodeRow(
    episode: com.aritxonly.bdfun.core.model.video.VideoEpisode, active: Boolean,
    onSelect: (com.aritxonly.bdfun.core.model.video.VideoEpisode) -> Unit, slots: VideoDetailTailSlots,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (active) colors.primary.copy(alpha = 0.12f) else Color.Transparent,
            )
            .clickable(interactionSource = null, indication = null) {
                onSelect(episode)
            }
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VideoCollectionEpisodeCover(episode, slots)
        Spacer(Modifier.width(8.dp))
        VideoCollectionEpisodeText(episode, active, Modifier.weight(1f))
    }
}

@Composable
internal fun VideoCollectionEpisodeCover(episode: com.aritxonly.bdfun.core.model.video.VideoEpisode, slots: VideoDetailTailSlots) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .width(84.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceContainer),
    ) {
        slots.cover(episode.cover, Modifier.fillMaxSize())
        if (episode.duration > 0) {
            Text(
                text = formatDuration(episode.duration),
                color = Color.White,
                fontSize = 10.sp,
                lineHeight = 11.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 5.dp, bottom = 4.dp),
            )
        }
    }
}

@Composable
internal fun VideoCollectionEpisodeText(
    episode: com.aritxonly.bdfun.core.model.video.VideoEpisode, active: Boolean, modifier: Modifier,
) {
    val colors = MiuixTheme.colorScheme
    Column(modifier = modifier) {
        Text(
            text = episode.title,
            color = if (active) colors.primary else colors.onBackground,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        val meta = buildList {
            if (episode.pubdate > 0) add(formatDateTime(episode.pubdate))
            if (episode.play > 0) add("${formatCount(episode.play)} 播放")
            if (episode.danmaku > 0) add("${formatCount(episode.danmaku)} 弹幕")
        }
        if (meta.isNotEmpty()) {
            Text(
                text = meta.joinToString(" · "),
                color = colors.onBackgroundVariant,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
