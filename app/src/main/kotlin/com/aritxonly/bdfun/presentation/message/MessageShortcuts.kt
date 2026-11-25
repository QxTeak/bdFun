package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val MessageGreen = Color(0xFF3CCF8E)
internal val MessagePink = Color(0xFFF45B86)
internal val MessageRed = Color(0xFFF04B4B)

typealias MessageFeedKind = com.aritxonly.bdfun.feature.message.application.MessageFeedKind

@Composable
internal fun MessageShortcut(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    unread: Int = 0,
    glyph: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            UnreadBadgeHost(unread) { glyph() }
        }
        Spacer(Modifier.height(7.dp))
        Text(text = label, color = colors.onBackground, fontSize = 12.sp)
    }
}
