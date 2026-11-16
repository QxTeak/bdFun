package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun MessageCircleButton(onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.dp, colors.onBackground.copy(alpha = 0.08f), CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MiuixIcons.Messages,
            contentDescription = "私信",
            tint = BrandPink,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
internal fun ChargeGlyph(size: androidx.compose.ui.unit.Dp) {
    AppGlyph(LucideIcons.ZapFilled, Color(0xFFFFC107), size = size)
}

@Composable
internal fun FollowCapsule(
    isSelf: Boolean,
    followed: Boolean,
    busy: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val shape = com.kyant.shapes.Capsule()
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(shape)
            .background(if (followed) colors.surfaceContainerHigh else colors.primary)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when {
                isSelf -> "编辑资料"
                followed -> "已关注"
                busy -> "处理中…"
                else -> "+ 关注"
            },
            color = if (followed) colors.onBackgroundVariant else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
