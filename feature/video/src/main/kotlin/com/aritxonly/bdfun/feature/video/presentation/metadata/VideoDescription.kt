package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.rememberManualProgress

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CollapsibleDescription(
    text: String,
    progress: Float,
    color: Color,
    backgroundColor: Color,
    onExpandableChange: () -> Unit,
    modifier: Modifier = Modifier,
    slots: VideoMetadataSlots,
) {
    val density = LocalDensity.current
    val linkColor = MiuixTheme.colorScheme.primary
    val links = slots.links(text, linkColor)
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    val style = androidx.compose.ui.text.TextStyle(
        color = color,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val annotatedText = androidx.compose.runtime.remember(text, linkColor, links.cacheKey) {
        links.annotate(text)
    }
    BoxWithConstraints(modifier) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val measured = measureVideoDescription(measurer, annotatedText, text, style, widthPx)
        val fullPx = measured.fullPx
        val collapsedPx = measured.collapsedPx
        val tailText = measured.tailText
        // 以「完整高度 > 两行高度」判定是否真的被折叠，比 hasVisualOverflow 稳
        LaunchedEffect(fullPx, collapsedPx) {
            if (fullPx > collapsedPx + 1f) onExpandableChange()
        }
        val p = progress.coerceIn(0f, 1f)
        // 给前两行下面再留 2dp 安全区：即使实际渲染行高和测量值有 1~2dp 偏差，
        // 模糊也进不到前两行
        val safeTopPx = collapsedPx + with(density) { 2.dp.toPx() }
        val targetPx = safeTopPx +
            (fullPx - safeTopPx).coerceAtLeast(0f) * p
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { targetPx.toDp() })
                .clipToBounds(),
        ) {
            val layers = VideoDescriptionLayersContent(
                fullPx, safeTopPx, p, tailText, annotatedText, style, backgroundColor, onExpandableChange,
            )
            VideoDescriptionTail(layers, links.annotate)
            VideoDescriptionFirstLines(layers)
        }
    }
}

/** 和简介一样的展开箭头：展开/收回都旋转，带补间动画。 */
@Composable
fun ExpandChevron(expanded: Boolean) {
    val colors = MiuixTheme.colorScheme
    val progress = rememberManualProgress(if (expanded) 1f else 0f, 300f)
    Box(
        modifier = Modifier.size(width = 24.dp, height = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        com.aritxonly.teak.common.icons.AppGlyph(
            com.aritxonly.teak.common.icons.LucideIcons.ChevronDown,
            tint = colors.onBackgroundVariant,
            modifier = Modifier.graphicsLayer { rotationZ = 180f * progress },
            size = 14.dp,
        )
    }
}

/** 多 P 分集：横向滑动查看、点一下切换。 */
