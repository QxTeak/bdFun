package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LargeVideoContent(video: VideoItem) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
        Text(
            text = video.title,
            color = colors.onBackground,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(5.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            UpBadge()
            if (video.isFollowed) {
                Spacer(Modifier.width(4.dp))
                FollowedBadge()
            }
            Spacer(Modifier.width(5.dp))
            Text(
                text = video.ownerName,
                color = colors.onBackgroundVariant,
                fontSize = 11.5.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(interactionSource = null, indication = null) {
                        navigateToSpace(context, video.ownerMid)
                    },
            )
        }
    }
}
