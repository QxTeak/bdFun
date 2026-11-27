package com.aritxonly.teak.common.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 底栏同款玻璃分段控件（全部 / 视频）。
 *
 * 指示器不再是“整块瞬间变色”，而是和底栏一样用弹簧在选项之间滑动；
 * 粉色底做了半透明处理，能透出底下的玻璃模糊。经典样式改成「文字 + 下划线」，
 * 两条分支各自独立组合，避免单函数过长。
 */
@Composable
fun AdaptiveScrollableTabs(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemWidth: Dp = 74.dp,
    materialAlpha: Float = 1f,
    /**
     * 连续位置（0.0 = 第一项，1.5 = 正从第二项滑向第三项）。
     * 传入后指示器和文字在布局阶段跟随手指/翻页，不再等页面切完才跳过去。
     */
    position: State<Float>? = null,
    /** 开启后仅在数量超过上限时滚动，关闭时保持首页原有布局。 */
    scrollable: Boolean = false,
    /** 向下取最近的正奇数；非正数按 1 处理，关闭滚动时忽略。 */
    maxVisibleItems: Int = 3,
) {
    if (options.isEmpty()) return
    if (scrollable && options.size > normalizedTabLimit(maxVisibleItems)) {
        ScrollableSegment(options, selected, onSelect, modifier, itemWidth, materialAlpha, position,
            maxVisibleItems)
        return
    }
    // 有连续位置时直接跟位置（位移只在布局期读，拖动时不再重组整条栏）；
    // 没有时才用弹簧追 selected，并保留原来的选中态语义。
    val animIndex: () -> Float
    val activeIndex: Int
    val positionSource = position
    if (positionSource != null) {
        val active by remember(positionSource) {
            derivedStateOf { positionSource.value.roundToInt().coerceIn(0, options.lastIndex) }
        }
        activeIndex = active
        animIndex = { positionSource.value }
    } else {
        // 没有连续位置（哔哩哔哩布局的顶部分段）时，指示器和底栏走同一套弹簧：
        // LowBouncy + MediumLow，切换时会轻微过冲，而不是跟着 pager 的减速曲线硬滑。
        val springIndex = animateFloatAsState(
            targetValue = selected.coerceIn(options.indices).toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
            label = "segmented_indicator_spring",
        )
        activeIndex = selected.coerceIn(options.indices)
        animIndex = { springIndex.value }
    }
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
        ClassicSegmentedBar(options, activeIndex, onSelect, modifier, itemWidth, animIndex)
    } else {
        GlassSegmentedBar(options, activeIndex, onSelect, modifier, itemWidth, materialAlpha, animIndex)
    }
}

@Composable
private fun ClassicSegmentedBar(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    itemWidth: Dp,
    animIndex: () -> Float,
) {
    val colors = MiuixTheme.colorScheme
    BoxWithConstraints(modifier = modifier.height(40.dp)) {
        // 经典布局的分段栏铺满整行，不再把固定宽度的几个 tab 挤在中间。
        val spread = maxWidth != Dp.Infinity
        val slotWidth = if (spread) maxWidth / options.size else itemWidth
        Box(
            modifier = Modifier
                .offset {
                    // 指示器位置在布局阶段跟随连续位置，手指拖动时不触发重组。
                    IntOffset(
                        (slotWidth.toPx() * animIndex() + slotWidth.toPx() * 0.34f).roundToInt(),
                        30.dp.roundToPx(),
                    )
                }
                .width(slotWidth * 0.32f)
                .height(3.dp)
                .background(colors.primary, Capsule()),
        )
        Row(modifier = if (spread) Modifier.fillMaxWidth() else Modifier) {
            options.forEachIndexed { index, label ->
                val active = index == selected
                Box(
                    modifier = (if (spread) Modifier.weight(1f) else Modifier.width(itemWidth))
                        .height(36.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label,
                        color = if (active) colors.primary else colors.onBackgroundVariant,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassSegmentedBar(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier,
    itemWidth: Dp,
    materialAlpha: Float,
    animIndex: () -> Float,
) {
    val colors = MiuixTheme.colorScheme
    val advancedMaterial = LocalTeakMaterialSpec.current
    val recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning)
    TeakGlassSurface(
        modifier = modifier.height(40.dp),
        shape = Capsule(),
        materialAlpha = materialAlpha,
        flatBackgroundAlpha = com.aritxonly.teak.common.material.TeakGlassDefaults.flatBackgroundAlpha(),
        recipe = recipe,
        backdrop = LocalTeakBackdrop.current,
    ) {
        Box(modifier = Modifier.padding(4.dp)) {
            // 滑动的粉色指示器（半透明，带一点高光描边）；位置在布局阶段读连续位置。
            Box(
                modifier = Modifier
                    .offset { IntOffset((itemWidth.toPx() * animIndex()).roundToInt(), 0) }
                    .width(itemWidth)
                    .height(32.dp)
                    .clip(Capsule())
                    .background(colors.primary.copy(alpha = 0.92f)),
            )
            Row {
                options.forEachIndexed { index, label ->
                    val active = index == selected
                    val textColor by animateColorAsState(
                        targetValue = if (active) Color.White else colors.onBackgroundVariant,
                        label = "segmented_text_color",
                    )
                    Box(
                        modifier = Modifier
                            .width(itemWidth)
                            .height(32.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
