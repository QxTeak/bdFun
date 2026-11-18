package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 宽屏使用上图下文，避免多栏时横向缩略图挤占标题空间。 */
@Composable
internal fun SearchResultGridCard(video: VideoItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    BoxWithConstraints(modifier.fillMaxWidth().clickable(onClick = onClick)) {
        val coverWidth = maxWidth
        Column {
            SearchVideoCover(video, coverWidth, coverWidth * 9f / 16f)
            Spacer(Modifier.height(8.dp))
            Text(video.title, color = colors.onBackground, fontSize = 14.sp, lineHeight = 20.sp,
                minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Text(video.ownerName, color = colors.onBackgroundVariant, fontSize = 12.sp,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${formatCount(video.play)} 播放 · ${formatCount(video.danmaku)} 弹幕",
                color = colors.onBackgroundVariant, fontSize = 11.sp, maxLines = 1,
                overflow = TextOverflow.Ellipsis)
        }
    }
}
