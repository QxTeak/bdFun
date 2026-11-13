package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.user.SentRecord
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.sent.application.*

@Composable
internal fun SentRecordRow(
    record: SentRecord,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isDanmaku = record.kind == "danmaku"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (isDanmaku) {
                            Color(0xFF3C9BD9).copy(alpha = 0.14f)
                        } else {
                            colors.primary.copy(alpha = 0.14f)
                        },
                    )
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text = if (isDanmaku) "弹幕" else "评论",
                    color = if (isDanmaku) Color(0xFF3C9BD9) else colors.primary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Normal,
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (isDanmaku) {
                    "视频内 ${formatVideoTime(record.videoTimeMs)}"
                } else {
                    formatSentAt(record.sentAt)
                },
                color = colors.onBackgroundVariant,
                fontSize = 10.5.sp,
            )
            Spacer(Modifier.weight(1f))
            Text(text = "›", color = colors.outline, fontSize = 14.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.text,
            color = colors.onBackground,
            fontSize = 13.5.sp,
            lineHeight = 19.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = record.title.ifBlank { record.bvid },
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (isDanmaku) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = "发送于 ${formatSentAt(record.sentAt)}",
                color = colors.onBackgroundVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
            )
        }
    }
}
