package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.shapes.Capsule

/** Badge is a sibling of the clipped button surface, overlapping its outer edge. */
@Composable
fun UnreadBadgeHost(count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // 角标只叠加绘制，不参与按钮尺寸，保证左右边距一致。
    Box(modifier) {
        content()
        if (count > 0) Text(
            text = if (count > 99) "99+" else count.toString(),
            color = MaterialTheme.colorScheme.onError, fontSize = 10.sp, lineHeight = 13.sp,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = 5.dp, y = (-3).dp)
                .semantics { contentDescription = "$count 条未读消息" }
                .background(MaterialTheme.colorScheme.error, Capsule())
                .padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
