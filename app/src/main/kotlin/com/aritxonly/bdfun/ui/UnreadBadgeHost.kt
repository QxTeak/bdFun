package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Badge is a sibling of the clipped button surface, overlapping its outer edge. */
@Composable
fun UnreadBadgeHost(count: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    // 角标只叠加绘制，不参与按钮尺寸，保证左右边距一致。
    Box(modifier) {
        content()
        if (count > 0) {
            UnreadCountBadge(
                count = count,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 5.dp, y = (-3).dp)
                    .semantics { contentDescription = "$count 条未读消息" },
            )
        }
    }
}

/**
 * 未读数量角标。
 *
 * 两处细节必须固定下来，否则观感会很粗糙：
 * 1. 高度定死、宽度只有下限 —— 单字符时宽高相等才是正圆，靠文字内边距撑开会得到椭圆。
 * 2. 字号行高一致并裁掉字体留白 —— 默认行高盒比字形高且上下不对称，数字会被压到底部。
 */
@Composable
internal fun UnreadCountBadge(
    count: Int,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.error,
    contentColor: Color = MaterialTheme.colorScheme.onError,
) {
    Box(
        modifier = modifier
            .height(BadgeHeight)
            .widthIn(min = BadgeHeight)
            .clip(CircleShape)
            .background(color),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = contentColor,
            style = BadgeTextStyle,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

private val BadgeHeight = 18.dp

private val BadgeTextStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 12.sp,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.Both,
    ),
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)
