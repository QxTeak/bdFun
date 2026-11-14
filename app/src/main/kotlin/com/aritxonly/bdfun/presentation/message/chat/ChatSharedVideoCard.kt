package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.core.model.message.SharedVideoInfo

@Composable
internal fun ChatSharedVideoCard(info: SharedVideoInfo) {
    val context = LocalContext.current
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) {
                navigateToVideo(
                    context,
                    VideoItem(
                        bvid = info.bvid,
                        aid = 0,
                        title = info.title,
                        cover = info.cover,
                        ownerName = "",
                        ownerFace = "",
                        ownerMid = 0,
                        play = info.play,
                        danmaku = info.danmaku,
                        duration = 0,
                        pubdate = 0,
                    ),
                )
            },
    ) {
        ChatSharedVideoCover(info)
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = info.title,
                color = colors.onBackground,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                maxLines = 2,
            )
            if (info.desc.isNotBlank() && info.desc != "-") {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = info.desc,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
