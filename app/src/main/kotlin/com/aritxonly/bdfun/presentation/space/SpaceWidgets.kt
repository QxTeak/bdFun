package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.formatCount
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal fun formatCountNoDecimal(value: Long): String =
    formatCount(value)
        .replace(Regex("\\.\\d+\\s*"), "")
        .replace(" ", "")

@Composable
internal fun SpaceSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    onMore: (() -> Unit)? = null,
) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            color = colors.onBackground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        if (onMore != null) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(interactionSource = null, indication = null, onClick = onMore)
                    .padding(start = 6.dp, end = 3.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "查看更多",
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                )
                Spacer(Modifier.width(2.dp))
                androidx.compose.foundation.Canvas(modifier = Modifier.size(7.dp, 10.dp)) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * 0.30f, size.height * 0.18f)
                        lineTo(size.width * 0.72f, size.height * 0.5f)
                        lineTo(size.width * 0.30f, size.height * 0.82f)
                    }
                    drawPath(
                        path = path,
                        color = colors.onBackgroundVariant,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 1.4.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            join = androidx.compose.ui.graphics.StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
internal fun SubmissionChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primary else colors.surfaceContainer)
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) Color.White.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(50),
            )
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else colors.onBackground,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
internal fun SpaceSubmissionCard(
    index: Int,
    isLast: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val isFirst = index == 0
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 16.dp else 0.dp,
        topEnd = if (isFirst) 16.dp else 0.dp,
        bottomStart = if (isLast) 16.dp else 0.dp,
        bottomEnd = if (isLast) 16.dp else 0.dp,
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = 12.dp,
                end = 12.dp,
                // 第一条视频和圆角卡片上边的距离再增加 2dp。
                top = if (isFirst) 15.dp else 0.dp,
                bottom = if (isLast) 6.dp else 0.dp,
            )
            .clip(shape)
            .background(colors.surfaceContainer),
    ) {
        content()
    }
}

@Composable
internal fun SpaceLoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        TeakLoading(size = 34.dp)
    }
}

@Composable
internal fun SpaceEmpty(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = MiuixTheme.colorScheme.onBackgroundVariant,
            fontSize = 13.sp,
        )
    }
}

@Composable
internal fun SpaceStat(label: String, value: Long, modifier: Modifier = Modifier) {
    val colors = MiuixTheme.colorScheme
    Column(
        modifier = modifier.offset(y = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            // 粉丝 / 关注 / 获赞不显示小数，例如 7.3 万 → 7万。
            text = formatCountNoDecimal(value),
            color = colors.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = label,
            color = colors.onBackgroundVariant,
            fontSize = 12.sp,
        )
    }
}
