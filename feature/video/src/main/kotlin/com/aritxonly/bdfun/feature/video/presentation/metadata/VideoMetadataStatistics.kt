package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.common.formatCount
import com.aritxonly.bdfun.core.common.formatDateTime

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun VideoMetadataTitle(content: VideoMetadataContent) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    val video = content.video
    androidx.compose.foundation.text.selection.SelectionContainer {
        Text(
            text = detail?.title ?: video.title,
            color = colors.onBackground,
            fontSize = 17.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun VideoMetadataStatistics(content: VideoMetadataContent, slots: VideoMetadataSlots) {
    val colors = MiuixTheme.colorScheme
    val detail = content.detail
    val video = content.video
    val onlineCount = content.onlineCount
    val onlineText = content.onlineText
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            slots.metric(true, colors.onBackgroundVariant, 14.dp)
            Spacer(Modifier.width(3.dp))
            Text(formatCount(detail?.play ?: video.play.toLong()),
                color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            slots.metric(false, colors.onBackgroundVariant, 14.dp)
            Spacer(Modifier.width(3.dp))
            Text(formatCount(detail?.danmaku ?: video.danmaku.toLong()),
                color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
        }
        detail?.pubdate?.takeIf { it > 0 }?.let {
            Text(formatDateTime(it), color = colors.onBackgroundVariant,
                fontSize = 12.sp, maxLines = 1)
        }
        if (onlineCount > 0 || onlineText.isNotBlank()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppGlyph(LucideIcons.Users, colors.onBackgroundVariant, size = 12.dp)
                Spacer(Modifier.width(3.dp))
                Text("${onlineText.ifBlank { formatCount(onlineCount) }} 人",
                    color = colors.onBackgroundVariant, fontSize = 12.sp, maxLines = 1)
            }
        }
    }
}
