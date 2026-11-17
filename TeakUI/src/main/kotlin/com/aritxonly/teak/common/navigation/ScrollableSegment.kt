package com.aritxonly.teak.common.navigation

import android.graphics.RenderEffect
import android.graphics.Shader
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.TeakMotion
import com.aritxonly.teak.common.material.*
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
        Box(modifier.height(40.dp)) { content() }
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
    BoxWithConstraints(Modifier.width(itemWidth * limit).clipToBounds()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val geometry = AdaptiveTabGeometry(options.size, widthPx, widthPx / (limit + .6f))
        val scroll = rememberScrollState()
        val scope = rememberCoroutineScope()
        val motion = remember(scroll) { AdaptiveTabMotion(scroll) }
        val current = (position?.value ?: selected.toFloat()).coerceIn(0f, options.lastIndex.toFloat())
        val active = current.roundToInt()
        val motionEnabled = TeakMotion.enabled
        val latestPosition by rememberUpdatedState(current)
        LaunchedEffect(current, geometry, motionEnabled) {
            motion.move(current, geometry, motion.initialized && position == null && motionEnabled)
            motion.initialized = true
        }
        LaunchedEffect(motion, geometry) {
            snapshotFlow { scroll.value }.collect { motion.recordManualPosition(latestPosition, geometry) }
        }
        val classic = TeakUiStyle.isClassic
        val colors = MiuixTheme.colorScheme
        val itemDp = with(density) { geometry.item.toDp() }
        val x = with(density) { motion.indicator(current, geometry).toDp() }
        Box(Modifier.offset(x + if (classic) itemDp * .34f else 0.dp, if (classic) 30.dp else 0.dp)
            .width(if (classic) itemDp * .32f else itemDp)
            .height(if (classic) 3.dp else 32.dp)
            .background(colors.primary.copy(alpha = if (classic) 1f else .92f), Capsule()))
        Row(Modifier.horizontalScroll(scroll).selectableGroup()) {
            options.forEachIndexed { index, label ->
                ScrollableSegmentLabel(label, index, active, geometry, scroll.value) { target ->
                    // 拖动浏览后再点当前频道也要归中，不能只等 selected 改变。
                    if (target.toFloat() == current) scope.launch {
                        motion.move(current, geometry, motionEnabled)
                    }
                    onSelect(target)
                }
            }
        }
    }
}

@Composable
private fun ScrollableSegmentLabel(
    label: String, index: Int, active: Int, geometry: AdaptiveTabGeometry,
    scroll: Int, onSelect: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val colors = MiuixTheme.colorScheme
    val classic = TeakUiStyle.isClassic
    val center = (index + .5f) * geometry.item - scroll
    // 仅处理仍有内容可滚动的一侧，首项和末项在端点保持清晰。
    val edgeDistance = minOf(
        if (scroll > 0) center else Float.POSITIVE_INFINITY,
        if (scroll < geometry.maxScroll - 1f) geometry.viewport - center else Float.POSITIVE_INFINITY,
    )
    val blurFraction = (1f - edgeDistance / (geometry.item * .6f)).coerceIn(0f, 1f)
    val blur = remember(density) {
        RenderEffect.createBlurEffect(1.5f * density.density, 1.5f * density.density, Shader.TileMode.DECAL)
            .asComposeRenderEffect()
    }
    Box(
        Modifier.width(with(density) { geometry.item.toDp() }).height(if (classic) 36.dp else 32.dp)
            .selectable(selected = index == active, role = Role.Tab, onClick = { onSelect(index) }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label, fontSize = 13.sp, maxLines = 1,
            color = if (index != active) colors.onBackgroundVariant else if (classic) colors.primary else Color.White,
            modifier = Modifier.graphicsLayer {
                renderEffect = if (blurFraction > .1f) blur else null
                alpha = 1f - blurFraction * .55f
            },
        )
    }
}
