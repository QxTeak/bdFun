package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.core.model.user.SentRecord
import com.aritxonly.bdfun.feature.mine.application.*

@Composable
internal fun MineRecent(cardShape: androidx.compose.ui.graphics.Shape, records: List<SentRecord>, navigation: MineNavigation) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer),
    ) {
        MineRow("我的评论与弹幕", onClick = navigation.onOpenSentHistory)
        // 最近三条评论 / 弹幕
        val recent = records
        recent.forEach { record ->
            MineRecentRecord(record, navigation)
        }
    }
}

@Composable
private fun MineRecentRecord(record: SentRecord, navigation: MineNavigation) {
    val colors = MiuixTheme.colorScheme
    val isDanmaku = record.kind == "danmaku"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
            ) {
                navigation.openRecord(record)
            }
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(
                    if (isDanmaku) {
                        MineBlue.copy(alpha = 0.14f)
                    } else {
                        colors.primary.copy(alpha = 0.14f)
                    },
                )
                .padding(horizontal = 6.dp, vertical = 1.dp),
        ) {
            Text(
                text = if (isDanmaku) "弹幕" else "评论",
                color = if (isDanmaku) MineBlue else colors.primary,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Normal,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = record.text,
            color = colors.onBackgroundVariant,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isDanmaku) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatVideoPosition(record.videoTimeMs),
                color = colors.onBackgroundVariant.copy(alpha = 0.8f),
                fontSize = 10.sp,
            )
        }
    }
}
