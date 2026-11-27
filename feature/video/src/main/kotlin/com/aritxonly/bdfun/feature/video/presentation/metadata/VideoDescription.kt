package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.rememberManualProgress
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 简介：折叠时两行 + 省略号，展开时整段显示。
 *
 * 正文始终按完整内容排版，只改外层裁剪高度：展开是从两行长到全文，
 * 收起是从全文缩回两行，两个方向都是同一条高度动画，不会再出现
 * 「文字先瞬间消失、框还在慢慢缩」的跳变。
 */
@Composable
internal fun CollapsibleDescription(
    text: String?,
    expanded: Boolean,
    color: Color,
    onExpandableChange: () -> Unit,
    modifier: Modifier = Modifier,
    slots: VideoMetadataSlots,
    /** 和简介共用同一条高度动画的尾部内容（视频标签）；为 null 时保持原简介行为。 */
    trailing: (@Composable () -> Unit)? = null,
) {
    val linkColor = MiuixTheme.colorScheme.primary
    val links = slots.links(text.orEmpty(), linkColor)
    val annotatedText = remember(text, linkColor, links.cacheKey) { links.annotate(text.orEmpty()) }
    val style = TextStyle(
        color = color,
        fontSize = 12.sp,
        lineHeight = 18.sp,
    )
    val density = LocalDensity.current
    val measurer = androidx.compose.ui.text.rememberTextMeasurer()
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        // 只量高度：不再像旧实现那样把正文切成两段分别绘制。
        val measured = remember(annotatedText, style, widthPx, text != null) {
            if (text == null) {
                Triple(0f, 0f, false)
            } else {
                val full = measurer.measure(
                    text = annotatedText, style = style, maxLines = Int.MAX_VALUE,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
                )
                val twoLine = measurer.measure(
                    text = annotatedText, style = style, maxLines = 2,
                    constraints = androidx.compose.ui.unit.Constraints(maxWidth = widthPx),
                )
                val fullPx = full.size.height.toFloat()
                val twoPx = if (full.lineCount > 2) twoLine.size.height.toFloat() else fullPx
                Triple(fullPx, twoPx, full.lineCount > 2)
            }
        }
        val fullPx = measured.first
        val twoLinePx = measured.second
        val hasMore = measured.third
        LaunchedEffect(hasMore) { if (hasMore) onExpandableChange() }
        val progress by animateFloatAsState(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = tween(420, easing = FastOutSlowInEasing),
            label = "description_expand",
        )
        if (trailing == null) {
            CollapsibleTextOnly(
                annotatedText = annotatedText,
                style = style,
                showText = text != null,
                twoLinePx = twoLinePx,
                fullPx = fullPx,
                hasMore = hasMore,
                progress = progress,
            )
        } else {
            // 正文和标签放进同一个裁剪容器：展开时整块高度一起长出来，
            // 标签不再单独 fade，也不会出现“标签先撑开、正文再慢慢收缩”的两套动画。
            CollapsibleDescriptionWithTrailing(
                annotatedText = annotatedText,
                style = style,
                showText = text != null,
                twoLinePx = twoLinePx,
                hasMore = hasMore,
                progress = progress,
                trailing = trailing,
            )
        }
    }
}

/** 只有正文时的原简介动画：两行与全文之间插值高度并裁剪。 */
@Composable
private fun CollapsibleTextOnly(
    annotatedText: AnnotatedString,
    style: TextStyle,
    showText: Boolean,
    twoLinePx: Float,
    fullPx: Float,
    hasMore: Boolean,
    progress: Float,
) {
    val density = LocalDensity.current
    val shownPx = twoLinePx + (fullPx - twoLinePx) * progress
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(with(density) { shownPx.toDp() })
            .clipToBounds(),
    ) {
        // 用无界高度测量正文：完整内容照常排版，超出外层高度的部分被裁掉，
        // 所以收起时是“往上收”，不是文字先没了再缩框。
        if (showText) {
            Text(
                text = annotatedText,
                style = style,
                maxLines = Int.MAX_VALUE,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.Top, unbounded = true),
            )
        }
        // 折叠态在第二行末尾补省略号；展开过程中淡出。
        if (hasMore) {
            Text(
                text = annotatedText,
                style = style,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = 1f - progress },
            )
        }
    }
}

/**
 * 正文 + 尾部内容（标签）共用一次高度动画。
 *
 * 标签是流式布局，高度要到测量阶段才知道，所以在 [SubcomposeLayout] 里按实际宽度
 * 量出整块内容高度，再在「两行正文高度」和「全部内容高度」之间插值并裁剪。
 */
@Composable
private fun CollapsibleDescriptionWithTrailing(
    annotatedText: AnnotatedString,
    style: TextStyle,
    showText: Boolean,
    twoLinePx: Float,
    hasMore: Boolean,
    progress: Float,
    trailing: @Composable () -> Unit,
) {
    SubcomposeLayout(Modifier.fillMaxWidth().clipToBounds()) { constraints ->
        val width = constraints.maxWidth
        val content = subcompose("description_content") {
            Column(Modifier.fillMaxWidth()) {
                if (showText) {
                    Text(
                        text = annotatedText,
                        style = style,
                        maxLines = Int.MAX_VALUE,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                trailing()
            }
        }.first().measure(Constraints(maxWidth = width))
        val fullContentPx = content.height.toFloat()
        val shownPx = twoLinePx + (fullContentPx - twoLinePx) * progress
        val shownHeight = shownPx.roundToInt().coerceAtLeast(0)
        // 折叠态第二行末尾的省略号仍然叠在正文上，不参与内容高度。
        val ellipsis = if (hasMore) {
            subcompose("description_ellipsis") {
                Text(
                    text = annotatedText,
                    style = style,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = 1f - progress },
                )
            }.first().measure(Constraints(maxWidth = width))
        } else null
        layout(width, shownHeight) {
            content.place(0, 0)
            ellipsis?.place(0, 0)
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
