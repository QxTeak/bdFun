package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageLoginPrompt(onNeedLogin: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("登录后可查看消息", color = colors.onBackgroundVariant, fontSize = 13.sp)
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                .clickable(onClick = onNeedLogin)
                .padding(horizontal = 26.dp, vertical = 11.dp),
        ) {
            Text("扫码登录", color = Color.White, fontSize = 14.sp)
        }
    }
}
