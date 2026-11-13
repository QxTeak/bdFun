package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.sent.application.*

@Composable
internal fun SentHistoryEmpty(cardShape: androidx.compose.ui.graphics.Shape) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .padding(vertical = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "还没有记录，发一条评论或弹幕试试",
            color = colors.onBackgroundVariant,
            fontSize = 12.5.sp,
        )
    }
}
