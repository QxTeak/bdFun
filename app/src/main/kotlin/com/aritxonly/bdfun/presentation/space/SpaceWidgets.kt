package com.aritxonly.bdfun

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.TeakLoading
import com.aritxonly.bdfun.ui.formatCount
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

private val decimalSuffixPattern = Regex("\\.\\d+\\s*")

internal fun formatCountNoDecimal(value: Long): String =
    formatCount(value)
        .replace(decimalSuffixPattern, "")
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
                AppGlyph(MiuixIcons.Basic.ArrowRight, colors.onBackgroundVariant, size = 10.dp)
            }
        }
    }
}

/**
 * 投稿页的视频行：不再套圆角矩形卡片，直接铺在页面上。
 * 左右留白与搜索结果页一致（12dp），避免视频内容被卡片再加一层内缩。
 */
@Composable
internal fun SpaceSubmissionVideoRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
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
