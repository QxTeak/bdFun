package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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

/** 视频卡右上角的小胶囊：充电专属（黄）与合集（主题色）。 */
@Composable
internal fun BoxScope.FeedVideoBadges(isCollection: Boolean, isCharge: Boolean) {
    if (!isCollection && !isCharge) return
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (isCharge) {
            FeedBadge(
                text = "充电",
                background = ChargeBadgeYellow,
                content = Color(0xFF3D2E00),
            )
        }
        if (isCollection) {
            FeedBadge(
                text = "合集",
                background = colors.primary.copy(alpha = 0.92f),
                content = Color.White,
            )
        }
    }
}

@Composable
private fun FeedBadge(text: String, background: Color, content: Color) {
    Box(
        modifier = Modifier
            .clip(Capsule())
            .background(background)
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Normal,
        )
    }
}

/** 充电专属的黄色底：和 B 站客户端一致，压暗文字保证在黄色上可读。 */
private val ChargeBadgeYellow = Color(0xFFFFC107)
