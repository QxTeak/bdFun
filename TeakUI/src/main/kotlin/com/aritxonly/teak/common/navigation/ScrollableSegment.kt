package com.aritxonly.teak.common.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.*
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

@Composable
internal fun ScrollableSegment(
    options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier,
    itemWidth: Dp, materialAlpha: Float, position: State<Float>?, maxVisibleItems: Int,
) {
    val classic = TeakUiStyle.isClassic
    val content: @Composable () -> Unit = {
        Box(Modifier.padding(if (classic) 0.dp else 4.dp)) {
            ScrollableSegmentContent(options, selected, onSelect, itemWidth, position, maxVisibleItems)
        }
    }
    if (classic) {
        Box(modifier.height(40.dp).clipToBounds()) { content() }
    } else {
        TeakGlassSurface(
            modifier = modifier.height(40.dp), shape = Capsule(), materialAlpha = materialAlpha,
            flatBackgroundAlpha = TeakGlassDefaults.flatBackgroundAlpha(),
            recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning),
            backdrop = LocalTeakBackdrop.current,
        ) { content() }
    }
}

@Composable
private fun ScrollableSegmentContent(
    options: List<String>, selected: Int, onSelect: (Int) -> Unit,
    itemWidth: Dp, position: State<Float>?, maxVisibleItems: Int,
) {
    val limit = normalizedTabLimit(maxVisibleItems)
    // 由外层胶囊统一裁切；内层裁切会在 4dp 内边距处提前截断提示字。
    BoxWithConstraints(Modifier.width(itemWidth * limit)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val geometry = AdaptiveTabGeometry(options.size, widthPx, widthPx / (limit + .6f))
        val scope = rememberCoroutineScope()
        val interactions = remember { MutableInteractionSource() }
        val pressed = interactions.collectIsPressedAsState()
        val dragged = interactions.collectIsDraggedAsState()
        val motion = remember(options) { AdaptiveTabMotion() }
        val fallback = animateFloatAsState(selected.toFloat(), tween(if (TeakMotion.enabled) 280 else 0))
        val external by rememberUpdatedState(position ?: fallback)
        val latestSelect by rememberUpdatedState(onSelect)
        val motionEnabled = TeakMotion.enabled
        val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
        val current = remember(motion, geometry) {
            derivedStateOf { motion.value(external.value).coerceIn(0f, options.lastIndex.toFloat()) }
        }
        val active by remember(current) { derivedStateOf { current.value.roundToInt() } }
        val select: (Int) -> Unit = { target ->
            motion.select(target, { external.value }, scope, motionEnabled, latestSelect)
        }
        val drag = rememberDraggableState { delta -> motion.drag(delta * direction, geometry.item, options.size) }
        Box(Modifier.width(maxWidth).draggable(drag, Orientation.Horizontal,
            interactionSource = interactions,
            onDragStarted = { motion.begin(external.value) },
            onDragStopped = { velocity -> select(motion.end(velocity * direction, geometry.item, options.size)) },
        )) {
            ScrollableSegmentIndicator(current, geometry, direction, motion, pressed.value || dragged.value)
            val itemDp = with(density) { geometry.item.toDp() }
            Row(Modifier.wrapContentWidth(Alignment.Start, unbounded = true)
                .width(itemDp * options.size).graphicsLayer {
                    translationX = -direction * geometry.scroll(current.value)
                }.selectableGroup()) {
                options.forEachIndexed { index, label ->
                    ScrollableSegmentLabel(label, index, active, geometry, current, select, interactions)
                }
            }
        }
    }
}

@Composable
private fun ScrollableSegmentLabel(
    label: String, index: Int, active: Int, geometry: AdaptiveTabGeometry,
    current: State<Float>, onSelect: (Int) -> Unit, interactions: MutableInteractionSource,
) {
    val density = LocalDensity.current
    val colors = MiuixTheme.colorScheme
    val classic = TeakUiStyle.isClassic
    val direction = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1f else 1f
    val blur = remember(density) {
        RenderEffect.createBlurEffect(1.5f * density.density, 1.5f * density.density, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    }
    Box(
        Modifier.width(with(density) { geometry.item.toDp() }).height(if (classic) 36.dp else 32.dp)
            .selectable(selected = index == active, interactionSource = interactions, indication = null,
                role = Role.Tab, onClick = { onSelect(index) }),
        contentAlignment = Alignment.Center,
    ) {
        val color by animateColorAsState(
            if (index != active) colors.onBackgroundVariant else if (classic) colors.primary else Color.White,
            tween(if (TeakMotion.enabled) 120 else 0),
        )
        Text(text = label, fontSize = 13.sp, maxLines = 1, color = color,
            modifier = Modifier.graphicsLayer {
                translationX = if (index == active) 0f else direction * geometry.edgeInset(index, current.value)
                alpha = 1f - geometry.edgeFade(index, current.value)
            })
        // 清晰/模糊两层交叉淡入，不在越过阈值时突然切换 RenderEffect。
        // 模糊层覆盖整个槽位，给字形周围留出采样空间，避免在字形包围盒处硬截断。
        Box(Modifier.matchParentSize().clearAndSetSemantics {}.graphicsLayer {
            renderEffect = blur
            translationX = if (index == active) 0f else direction * geometry.edgeInset(index, current.value)
            alpha = geometry.edgeFade(index, current.value) * .45f
        }, contentAlignment = Alignment.Center) {
            Text(text = label, fontSize = 13.sp, maxLines = 1, color = color)
        }
    }
}

@Composable
private fun ScrollableSegmentIndicator(
    current: State<Float>, geometry: AdaptiveTabGeometry, direction: Float,
    motion: AdaptiveTabMotion, pressed: Boolean,
) {
    val density = LocalDensity.current
    val item = with(density) { geometry.item.toDp() }
    val classic = TeakUiStyle.isClassic
    val color = MiuixTheme.colorScheme.primary
    val enabled = TeakMotion.enabled
    val pressScale = animateFloatAsState(
        if (pressed && enabled) FloatingTabMotionDefaults.PressedScale else 1f,
        if (enabled) spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium) else tween(0),
    )
    val stretch = animateFloatAsState(
        if (enabled) floatingTabStretch(motion.velocity, 1f) else 0f,
        if (enabled) spring(Spring.DampingRatioNoBouncy, Spring.StiffnessHigh) else tween(0),
    )
    Box(Modifier.width(if (classic) item * .32f else item)
        .height(if (classic) 3.dp else 32.dp)
        .graphicsLayer {
            translationX = direction * (geometry.indicator(current.value) + if (classic) geometry.item * .34f else 0f)
            translationY = if (classic) 30.dp.toPx() else 0f
            // 中间位置保持不动，只有胶囊形状随速度拉伸并回弹。
            scaleX = pressScale.value * (1f + stretch.value)
            scaleY = pressScale.value / (1f + stretch.value * .35f)
        }.background(color.copy(alpha = if (classic) 1f else .92f), Capsule()))
}
