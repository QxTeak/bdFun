package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.navigateToSpace
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun GridVideoContent(video: VideoItem, showOwner: Boolean) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current

    Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 7.dp, bottom = 8.dp)) {
        Text(
            text = video.title,
            color = colors.onBackground,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            ),
            modifier = Modifier.heightIn(min = TitleBlockHeight),
        )

        if (showOwner) {
            Spacer(Modifier.height(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // UP 名称可点：进 UP 空间页（点击被这里消费，不会触发整卡进视频）
                Row(
                    modifier = Modifier
                        .clip(Capsule())
                        .clickable(interactionSource = null, indication = null) {
                            navigateToSpace(
                                context,
                                video.ownerMid,
                            )
                        }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UpBadge()
                    if (video.isFollowed) {
                        Spacer(Modifier.width(4.dp))
                        FollowedBadge()
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = video.ownerName,
                        color = colors.onBackgroundVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
