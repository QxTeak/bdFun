package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ExpandableSign(text: String) {
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        color = colors.onBackgroundVariant,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    )
    val ui = remember(text) { SpaceSignStateHolder() }
    // 和视频页简介同一个 420ms 补间，箭头旋转与高度展开同步。
    val progress = com.aritxonly.teak.common.chrome.rememberManualProgress(
        target = if (ui.expanded) 1f else 0f,
        durationMs = 420f,
        easing = androidx.compose.animation.core.FastOutSlowInEasing,
    )
    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // 右侧给展开图标留出位置，文字不会跑到图标下面。
        val widthPx = with(density) { (maxWidth - 24.dp).toPx() }
            .toInt()
            .coerceAtLeast(1)
        val fullLayout = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = Int.MAX_VALUE,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val oneLineLayout = measurer.measure(
            text = androidx.compose.ui.text.AnnotatedString(text),
            style = style,
            maxLines = 1,
            constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
        )
        val collapsedPx = oneLineLayout.size.height.toFloat()
        val fullPx = fullLayout.size.height.toFloat()
        val expandable = fullPx > collapsedPx + 1f
        val targetPx = collapsedPx + (fullPx - collapsedPx) * progress
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { targetPx.toDp() })
                .clipToBounds(),
        ) {
            Text(
                text = text,
                style = style,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier.padding(end = 24.dp),
            )
        }
        if (expandable) {
            // 和视频页简介一样：只在第一行右上角放一个箭头，点一下平滑展开/收起。
            SpaceSignToggle(ui, progress)
        }
    }
}

internal class SpaceSignStateHolder { var expanded by mutableStateOf(false) }
