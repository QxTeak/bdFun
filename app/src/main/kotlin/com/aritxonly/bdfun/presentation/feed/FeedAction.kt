package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
internal fun FeedAction(
    icon: @Composable () -> Unit,
    count: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            if (count > 0) {
                Spacer(Modifier.width(4.dp))
                Text(
                    text = formatCount(count.toLong()),
                    color = tint ?: colors.onBackgroundVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.offset(y = 1.dp),
                )
            }
        }
    }
}

/**
 * 评论气泡：圆角矩形 + 左下角小尾巴（原来只有方框，容易被看成"图片"图标）。
 * 描边宽度和分享/点赞图标同一视觉重量。
 */
