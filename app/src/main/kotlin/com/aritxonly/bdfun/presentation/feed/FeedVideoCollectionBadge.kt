package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.FeedVideoCollectionBadge(isCollection: Boolean) {
    val colors = MiuixTheme.colorScheme
    if (isCollection) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 8.dp, end = 8.dp)
                .width(42.dp)
                .clip(Capsule())
                .background(colors.primary.copy(alpha = 0.92f))
                .padding(horizontal = 7.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "合集",
                color = Color.White,
                fontSize = 9.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Normal,
            )
        }
    }
}
