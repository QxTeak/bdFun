package com.aritxonly.bdfun.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 和直播页分区同款的胶囊：32dp 高、全圆角、选中主色填充白字、未选中 surfaceContainer。
 *
 * 空间投稿分类、收藏夹等横向筛选统一用它，避免各处各写一套圆角/字号/内边距。
 */
@Composable
internal fun TeakCapsuleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 32.dp,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .height(height)
            .clip(Capsule())
            .background(if (selected) colors.primary.copy(alpha = 0.92f) else colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = horizontalPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackgroundVariant,
            fontSize = fontSize,
            maxLines = 1,
        )
    }
}
