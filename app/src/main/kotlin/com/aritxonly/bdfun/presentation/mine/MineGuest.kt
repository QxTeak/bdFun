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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.aritxonly.bdfun.feature.mine.application.*

@Composable
internal fun MineGuest(cardShape: androidx.compose.ui.graphics.Shape, onLogin: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surfaceContainer)
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "尚未登录",
            color = colors.onBackground,
            fontSize = 17.sp,
            fontWeight = FontWeight.Normal,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "登录后可同步关注、收藏与历史记录",
            color = colors.onBackgroundVariant,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(colors.primary)
                .clickable { onLogin() }
                .padding(horizontal = 28.dp, vertical = 11.dp),
        ) {
            Text(
                text = "扫码登录",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
