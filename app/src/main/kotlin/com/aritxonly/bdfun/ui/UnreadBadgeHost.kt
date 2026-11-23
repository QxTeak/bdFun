package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Badge is a sibling of the clipped button surface, overlapping its outer edge. */
@Composable
fun UnreadBadgeHost(count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // 角标只叠加绘制，不参与按钮尺寸，保证左右边距一致。
    Box(modifier) {
        content()
        if (count > 0) {
            // 未读角标必须是正圆：用固定边长 Box + CircleShape，而不是靠文字 padding 撑出来的胶囊。
            val badgeSize = when {
                count > 99 -> 20.dp
                count > 9 -> 18.dp
                else -> 16.dp
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-3).dp)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.error)
                    .semantics { contentDescription = "$count 条未读消息" },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    color = MaterialTheme.colorScheme.onError,
                    fontSize = if (count > 99) 7.sp else 9.sp,
                    lineHeight = 9.sp,
                    maxLines = 1,
                )
            }
        }
    }
}
