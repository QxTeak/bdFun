package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivateChatLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            "加载中…",
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun PrivateChatLoadError(detail: String?, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                detail?.takeIf { it.isNotBlank() }?.let { "消息加载失败：$it" } ?: "消息加载失败",
                color = MiuixTheme.colorScheme.onBackgroundVariant,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "点击重试",
                color = MiuixTheme.colorScheme.primary,
                fontSize = 13.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onRetry() }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}
