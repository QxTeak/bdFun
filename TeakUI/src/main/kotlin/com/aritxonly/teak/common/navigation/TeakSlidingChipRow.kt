package com.aritxonly.teak.common.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.abs
import kotlin.math.roundToInt

/** 独立胶囊标签：每个标签仍是一枚分开的胶囊，[TeakSlidingChipRow] 只负责让选中底色在它们之间滑动。 */
@Immutable
data class TeakChip(val key: Any, val label: String)

/**
 * 一条会滑动的选中底色：
 * - 位置用一根带轻微回弹的弹簧，撞到目标会「顿」一下（距离越远阻尼越大，不会冲过头）；
 * - 位移速度换成水平拉伸，前缘先探出去、速度归零就收回，同时按拉伸量轻微压扁高度，
 *   橡皮被甩出去的感觉来自形状变化，而不是把两个边缘拉断。
 *
 * 和分段控件（[AdaptiveScrollableTabs]）的区别只在观感：标签之间保留真实空隙、各自一枚胶囊，
 * 粉色块从一枚滑到另一枚时会经过空隙；但动效沿用分段那套弹簧，不再「点谁谁瞬间变色」。
 *
 * 位置在组合期就用 [rememberTextMeasurer] 量好，而不是等 `onGloballyPositioned` 回来：
 * 底色和胶囊都画在同一层 draw 里，首帧就得知道坐标，否则会先闪一帧没有底色的标签。
 *
 * [bleed] 用来抵消外层内容边距，让这一行能一直滚到屏幕边缘再被裁切（0 表示不扩）。
 */
@Composable
fun TeakSlidingChipRow(
    chips: List<TeakChip>,
    selectedKey: Any?,
    onSelect: (TeakChip) -> Unit,
    modifier: Modifier = Modifier,
    bleed: Dp = 0.dp,
    height: Dp = 32.dp,
    horizontalPadding: Dp = 16.dp,
    spacing: Dp = 8.dp,
    verticalPadding: Dp = 0.dp,
    fontSize: TextUnit = 13.sp,
) {
    if (chips.isEmpty()) return
    val colors = MiuixTheme.colorScheme
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    // 量宽度必须用「Text 真正会用的那份样式」：直接 new 一个 TextStyle 会丢掉主题字体/字距，
    // 胶囊就会比文字宽或窄一截。
    val baseLabelStyle = LocalTextStyle.current
    val labelStyle = remember(baseLabelStyle, fontSize) {
        baseLabelStyle.merge(TextStyle(fontSize = fontSize))
    }
    val padPx = with(density) { horizontalPadding.roundToPx() }
    val gapPx = with(density) { spacing.roundToPx() }
    val heightPx = with(density) { height.toPx() }
    // 走 bleed 时首枚胶囊还要再让出 bleed*2，和原来的横向卡片行同一套几何。
    val startPx = with(density) { (bleed * 2).toPx() }.toInt()
    val chipWidths = remember(chips, labelStyle, density) {
        chips.map {
            measurer.measure(AnnotatedString(it.label), labelStyle, maxLines = 1).size.width + padPx * 2
        }
    }
    val offsets = remember(chipWidths, gapPx, startPx) {
        var x = startPx
        IntArray(chipWidths.size) { index ->
            val value = x
            x += chipWidths[index] + if (index == chipWidths.lastIndex) 0 else gapPx
            value
        }
    }
    val selectedIndex = chips.indexOfFirst { it.key == selectedKey }
    val targetLeftPx = if (selectedIndex >= 0) offsets[selectedIndex].toFloat() else 0f
    val targetWidthPx = if (selectedIndex >= 0) chipWidths[selectedIndex].toFloat() else 0f
    val indicatorLeft = remember { Animatable(targetLeftPx) }
    val indicatorWidth = remember { Animatable(targetWidthPx) }
    // 位移速度（px/s）只用来算「甩出去时被拉多长」，不参与位置本身，所以不会把块拉断。
    val travelSpeed = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(targetLeftPx, targetWidthPx, selectedIndex) {
        if (selectedIndex < 0) return@LaunchedEffect
        if (!TeakMotion.enabled) {
            indicatorLeft.snapTo(targetLeftPx)
            indicatorWidth.snapTo(targetWidthPx)
            travelSpeed.floatValue = 0f
            return@LaunchedEffect
        }
        // 距离越远阻尼越大：近处弹性足一点，长距离只留一点点，不会冲过头。
        // 刚度放低到 320：起步和收尾都更慢，中段再快，曲线比原来更“非线性”。
        val distance = abs(targetLeftPx - indicatorLeft.value)
        val damping = (0.78f + distance / 1200f * 0.10f).coerceIn(0.78f, 0.88f)
        var previous = indicatorLeft.value
        var previousAt = System.nanoTime()
        coroutineScope {
            launch {
                indicatorLeft.animateTo(
                    targetLeftPx,
                    spring(dampingRatio = damping, stiffness = 320f),
                ) {
                    val now = System.nanoTime()
                    val dt = ((now - previousAt) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
                    // 单帧速度抖得厉害，做一点低通，拉伸才不会一跳一跳。
                    travelSpeed.floatValue =
                        travelSpeed.floatValue * 0.45f + ((value - previous) / dt) * 0.55f
                    previous = value
                    previousAt = now
                }
                travelSpeed.floatValue = 0f
            }
            launch {
                indicatorWidth.animateTo(targetWidthPx, spring(dampingRatio = 0.78f, stiffness = 420f))
            }
        }
    }
    val chipBackground = colors.surfaceContainer
    val indicatorColor = colors.primary.copy(alpha = 0.92f)
    val scrollState = rememberScrollState()
    // 甩出去的时候两边对称鼓出去一点：速度越快拉得越长，停下就收回来。
    val stretchNow: () -> Float = {
        val speed = travelSpeed.floatValue.coerceIn(-4000f, 4000f)
        (abs(speed) / 1000f * 24f).coerceIn(0f, 36f)
    }
    val blockLeft: () -> Float = { indicatorLeft.value - stretchNow() / 2f }
    val blockWidth: () -> Float = { (indicatorWidth.value + stretchNow()).coerceAtLeast(4f) }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val viewport = maxWidth
        Box(
            modifier = Modifier
                .padding(vertical = verticalPadding)
                .offset(x = -bleed)
                .requiredWidth(viewport + bleed * 2)
                .horizontalScroll(scrollState),
        ) {
            Row(
                modifier = Modifier.height(height),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.width(bleed * 2))
                chips.forEachIndexed { index, chip ->
                    TeakChipLabel(
                        chip = chip,
                        selected = index == selectedIndex,
                        widthPx = chipWidths[index],
                        heightPx = heightPx.roundToInt(),
                        offsetPx = offsets[index].toFloat(),
                        style = labelStyle,
                        background = chipBackground,
                        indicator = indicatorColor,
                        blockLeft = blockLeft,
                        blockWidth = blockWidth,
                        onClick = { onSelect(chip) },
                    )
                    // 间距自己加而不是用 spacedBy：Row 里还有开头的 Spacer，spacedBy 会把它也算一段，
                    // 整排胶囊就会比量出来的坐标右移一个 spacing。
                    if (index != chips.lastIndex) Spacer(Modifier.width(spacing))
                }
            }
        }
    }
}

/**
 * 一枚标签自己画灰底和粉色块、自己缩放。
 *
 * 底色和粉色块必须画在标签自己的 draw 里：选中时整枚胶囊要一起缩一下，
 * 如果胶囊还由外层 Row 画，缩放只作用到文字，粉色块也不会跟着胶囊收缩。
 *
 * 粉色块按这枚胶囊的圆角路径裁切：它只在标签内部可见，两枚之间的空隙是被「盖住」的，
 * 而不是在整行上直接擦过去。高度始终等于胶囊高度，不做纵向压缩。
 */
@Composable
private fun TeakChipLabel(
    chip: TeakChip,
    selected: Boolean,
    widthPx: Int,
    heightPx: Int,
    offsetPx: Float,
    style: TextStyle,
    background: Color,
    indicator: Color,
    blockLeft: () -> Float,
    blockWidth: () -> Float,
    onClick: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val spec = when {
        !TeakMotion.enabled -> snap<Color>()
        selected -> tween(durationMillis = 110, delayMillis = 60)
        else -> tween(durationMillis = 110)
    }
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else colors.onBackgroundVariant,
        animationSpec = spec,
        label = "chip-label-color",
    )
    val shrink = remember { Animatable(1f) }
    var wasSelected by remember { mutableStateOf(selected) }
    var settledOnce by remember { mutableStateOf(false) }
    LaunchedEffect(selected) {
        // 首帧不播：进页面时选中项不该自己缩一下。
        if (!settledOnce) {
            settledOnce = true
            wasSelected = selected
            return@LaunchedEffect
        }
        if (!TeakMotion.enabled) {
            shrink.snapTo(1f)
            wasSelected = selected
            return@LaunchedEffect
        }
        if (selected) {
            // 整枚胶囊先缩一点，再带一点回弹回到原尺寸。
            shrink.animateTo(0.95f, spring(dampingRatio = 0.55f, stiffness = 700f))
            shrink.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 320f))
        } else if (wasSelected) {
            shrink.animateTo(0.98f, tween(durationMillis = 70))
            shrink.animateTo(1f, spring(dampingRatio = 0.85f, stiffness = 380f))
        }
        wasSelected = selected
    }
    // 连续圆角胶囊的路径只跟尺寸有关：记住它，别在每帧的 draw 里重新 createOutline。
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val capsulePath = remember(widthPx, heightPx, layoutDirection) {
        continuousCapsulePath(Size(widthPx.toFloat(), heightPx.toFloat()), layoutDirection, density)
    }
    // 粉色块宽度动画时每帧都在变；复用同一个 Path，只在不宽的时候重画几何。
    val blockPath = remember { Path() }
    var cachedBlockWidth by remember { mutableFloatStateOf(-1f) }
    Box(
        modifier = Modifier
            // 宽高按量出来的像素写死，而不是 px→dp→px 绕一圈：每枚差一像素，
            // 一排放下来就会整体错位，粉色块和胶囊对不上。
            .chipSize(widthPx, heightPx)
            .graphicsLayer {
                scaleX = shrink.value
                scaleY = shrink.value
            }
            .drawBehind {
                val h = heightPx.toFloat()
                // 连续圆角（Kyant Capsule / Miuix 同源），不是 drawRoundRect 的普通圆角。
                drawPath(capsulePath, background)
                val left = blockLeft() - offsetPx
                val blockW = blockWidth()
                if (blockW > 0f && left + blockW > 0f && left < widthPx) {
                    if (blockW != cachedBlockWidth) {
                        blockPath.rewind()
                        blockPath.addPath(continuousCapsulePath(Size(blockW, h), layoutDirection, density))
                        cachedBlockWidth = blockW
                    }
                    clipPath(capsulePath) {
                        translate(left, 0f) { drawPath(blockPath, indicator) }
                    }
                }
            }
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = chip.label,
            color = textColor,
            style = style,
            maxLines = 1,
        )
    }
}

private fun Modifier.chipSize(widthPx: Int, heightPx: Int): Modifier = layout { measurable, _ ->
    val constraints = Constraints(
        minWidth = widthPx, maxWidth = widthPx,
        minHeight = heightPx, maxHeight = heightPx,
    )
    val placeable = measurable.measure(constraints)
    layout(widthPx, heightPx) { placeable.place(0, 0) }
}

/**
 * 连续圆角胶囊路径：和 Miuix 用的 `com.kyant.shapes.Capsule` 同一套形状（Continuous），
 * 不是普通圆角。调用方只在尺寸变化时构建一次。
 */
private fun continuousCapsulePath(
    size: Size,
    layoutDirection: androidx.compose.ui.unit.LayoutDirection,
    density: androidx.compose.ui.unit.Density,
): Path = Path().apply {
    val capsule = com.kyant.shapes.Capsule(com.kyant.shapes.RoundedCornerStyle.Continuous)
    when (val outline = capsule.createOutline(size, layoutDirection, density)) {
        is Outline.Generic -> addPath(outline.path)
        is Outline.Rounded -> addRoundRect(outline.roundRect)
        else -> addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
    }
}
