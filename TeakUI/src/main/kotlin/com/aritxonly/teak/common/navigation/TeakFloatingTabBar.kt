package com.aritxonly.teak.common.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.GlassMaterialInteraction
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign

@Immutable
data class TeakFloatingTabItem(
    val key: String,
    val label: String,
    val selectedIcon: Painter,
    val unselectedIcon: Painter,
    val enabled: Boolean = true,
    val iconScale: Float = 1f,
    /** Null hides the badge; an empty label draws the red-dot variant. */
    val badge: String? = null,
)

enum class TeakFloatingTabLayout {
    Horizontal,
    Stacked,
}

/**
 * Deadliner's compact floating bottom tab bar for the MIUIX design system. A caller that enables
 * advanced material should register its content with a [LayerBackdrop] and either pass that
 * backdrop here or provide it through [LocalTeakBackdrop].
 *
 * 首页悬浮胶囊底栏移植并改造自 HyperPiliPlus 的原生实现，
 * 统一收进 TeakUI 供多个 App 复用；来源记录见 docs/PROVENANCE.md。
 */
@Composable
fun TeakFloatingTabBar(
    items: List<TeakFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (TeakFloatingTabItem) -> Unit,
    modifier: Modifier = Modifier,
    layout: TeakFloatingTabLayout = TeakFloatingTabLayout.Horizontal,
    selectionVisible: Boolean = true,
    selectedContentColor: Color = Color.Unspecified,
    backdrop: Backdrop? = LocalTeakBackdrop.current,
) {
    if (items.isEmpty()) return

    val selectedIndex = items.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)
    val advancedMaterial = LocalTeakMaterialSpec.current
    val themedContentColor = MiuixTheme.colorScheme.onSurfaceContainer
    val isDark = themedContentColor.luminance() > 0.5f
    val contentColor = if (isDark) Color.White else Color.Black
    val motion = remember { FloatingTabMotionState(selectedIndex) }
    val panelReturnOffset = remember { Animatable(0f) }
    var directPanelDragOffset: Float? by remember { mutableStateOf(null) }
    val motionScope = rememberCoroutineScope()
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr

    LaunchedEffect(selectedIndex) {
        motion.syncExternalSelection(selectedIndex)
    }

    val indicatorPressScale by animateFloatAsState(
        targetValue = if (motion.isPressed) {
            FloatingTabMotionDefaults.PressedScale
        } else {
            1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_indicator_press_scale",
    )
    val selectionAlpha by animateFloatAsState(
        targetValue = if (selectionVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_selection_visibility",
    )

    BoxWithConstraints(
        modifier = modifier
            .widthIn(
                max = minOf(
                    TeakFloatingTabBarDefaults.MaximumWidth,
                    TeakFloatingTabBarDefaults.MaximumItemWidth * items.size,
                ),
            )
            .fillMaxWidth()
            .height(TeakFloatingTabBarDefaults.CurrentHeight),
    ) {
        val density = LocalDensity.current
        val panelDragFraction = floatingTabPanelDragFraction(
            dragOffsetPx = directPanelDragOffset ?: panelReturnOffset.value,
            panelWidthPx = constraints.maxWidth.toFloat(),
        )
        val panelOffsetPx = with(density) {
            FloatingTabMotionDefaults.MaximumPanelOffsetDp.dp.toPx()
        } * sign(panelDragFraction) * EaseOut.transform(abs(panelDragFraction))
        val stretch by animateFloatAsState(
            targetValue = floatingTabStretch(
                velocityPxPerSecond = motion.velocityPxPerSecond,
                tabWidthPx = constraints.maxWidth.toFloat() / items.size,
            ),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh,
            ),
            label = "floating_tab_indicator_velocity_stretch",
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(panelOffsetPx.roundToInt(), 0) }
                .teakFloatingNavigationShadow(
                    shape = TeakFloatingTabBarDefaults.ContainerShape,
                    isDark = isDark,
                ),
        ) {
            TeakGlassSurface(
                modifier = Modifier.fillMaxSize(),
                shape = TeakFloatingTabBarDefaults.ContainerShape,
                recipe = TeakGlassRecipes.floatingNavigation(advancedMaterial.fineTuning),
                interaction = GlassMaterialInteraction(
                    refractionScale = if (motion.isPressed) 1.06f else 1f,
                    highlightScale = if (motion.isPressed) 1.04f else 1f,
                ),
                backdrop = backdrop,
            ) {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = TeakFloatingTabBarDefaults.HorizontalContentPadding,
                            vertical = TeakFloatingTabBarDefaults.VerticalContentPadding,
                        ),
                ) {
                    val itemWidth = maxWidth / items.size
                    val itemWidthPx = with(density) { itemWidth.toPx() }
                    val indicatorColor = if (isDark) {
                        Color.White.copy(alpha = 0.13f)
                    } else {
                        Color.Black.copy(alpha = 0.075f)
                    }
                    val transformOriginX = floatingTabTransformOriginX(
                        velocityPxPerSecond = motion.velocityPxPerSecond,
                        stretch = stretch,
                    )

                    Box(
                        modifier = Modifier
                            .offset(
                                x = itemWidth * motion.visualPosition -
                                    TeakFloatingTabBarDefaults.IndicatorHorizontalOverflow,
                            )
                            .width(
                                itemWidth +
                                    TeakFloatingTabBarDefaults.IndicatorHorizontalOverflow * 2,
                            )
                            .fillMaxHeight()
                            .graphicsLayer {
                                alpha = selectionAlpha
                                scaleX = indicatorPressScale * (1f + stretch)
                                scaleY = indicatorPressScale
                                transformOrigin = TransformOrigin(transformOriginX, 0.5f)
                            }
                            .clip(TeakFloatingTabBarDefaults.IndicatorShape)
                            .background(indicatorColor),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(items, itemWidthPx, isLtr, selectionVisible) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    if (!selectionVisible) return@awaitEachGesture
                                    val indicatorStartPx = if (isLtr) {
                                        motion.visualPosition * itemWidthPx
                                    } else {
                                        (items.lastIndex - motion.visualPosition) * itemWidthPx
                                    }
                                    if (down.position.x !in indicatorStartPx..(indicatorStartPx + itemWidthPx)) {
                                        return@awaitEachGesture
                                    }

                                    motion.press()
                                    val downX = down.position.x
                                    val startPosition = motion.visualPosition
                                    val velocityTracker = VelocityTracker()
                                    velocityTracker.addPosition(down.uptimeMillis, down.position)
                                    var dragging = false
                                    var cancelled = false

                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull { it.id == down.id }
                                        if (change == null) {
                                            cancelled = true
                                            break
                                        }
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        val totalDragPx = change.position.x - downX
                                        if (!dragging && abs(totalDragPx) > viewConfiguration.touchSlop) {
                                            dragging = true
                                            motion.startDrag()
                                        }
                                        if (dragging) {
                                            change.consume()
                                            directPanelDragOffset = totalDragPx
                                            val physicalVelocity = velocityTracker.calculateVelocity().x
                                            val logicalDirection = if (isLtr) 1f else -1f
                                            motion.dragTo(
                                                positionInTabs = startPosition +
                                                    totalDragPx / itemWidthPx * logicalDirection,
                                                tabCount = items.size,
                                                velocityPxPerSecond = physicalVelocity,
                                            )
                                        }
                                        if (!change.pressed) break
                                    }

                                    if (dragging && !cancelled) {
                                        val targetIndex = resolveFloatingTabTarget(
                                            position = motion.visualPosition,
                                            tabCount = items.size,
                                            enabledIndices = items.indices.filter { items[it].enabled },
                                        )
                                        motion.finishGesture(targetIndex)
                                        motionScope.launch { motion.settleTo(targetIndex) }
                                        onItemSelected(items[targetIndex])
                                    } else {
                                        motion.cancelGesture()
                                        if (cancelled) {
                                            motionScope.launch { motion.settleTo(selectedIndex) }
                                        }
                                    }
                                    val releasedPanelOffset = directPanelDragOffset ?: 0f
                                    motionScope.launch {
                                        panelReturnOffset.snapTo(releasedPanelOffset)
                                        directPanelDragOffset = null
                                        panelReturnOffset.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioNoBouncy,
                                                stiffness = Spring.StiffnessMedium,
                                                visibilityThreshold = 0.5f,
                                            ),
                                        )
                                    }
                                }
                            }
                            .selectableGroup(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items.forEachIndexed { index, item ->
                            val selected = selectionVisible && index == selectedIndex
                            FloatingTabItemContent(
                                item = item,
                                selected = selected,
                                selectedPressProgress = if (selected && motion.isPressed) 1f else 0f,
                                contentColor = contentColor,
                                selectedContentColor = selectedContentColor,
                                layout = layout,
                                onClick = { onItemSelected(item) },
                                modifier = Modifier
                                    .width(itemWidth)
                                    .fillMaxHeight(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingTabItemContent(
    item: TeakFloatingTabItem,
    selected: Boolean,
    selectedPressProgress: Float,
    contentColor: Color,
    selectedContentColor: Color,
    layout: TeakFloatingTabLayout,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val targetColor = when {
        !item.enabled -> contentColor.copy(alpha = 0.22f)
        // 选中项用品牌色（图标 + 文字一起染色，和 B 站一致）
        selected && selectedContentColor != Color.Unspecified -> selectedContentColor
        else -> contentColor
    }
    val animatedContentColor by animateColorAsState(
        targetValue = targetColor,
        label = "floating_tab_content_color",
    )
    val animatedIconScale by animateFloatAsState(
        targetValue = floatingTabSelectedIconScale(selectedPressProgress),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "floating_tab_selected_icon_scale",
    )

    val interactionSource = remember { MutableInteractionSource() }
    val itemModifier = modifier
        .clip(TeakFloatingTabBarDefaults.IndicatorShape)
        .semantics { contentDescription = item.label }
        .selectable(
            selected = selected,
            enabled = item.enabled,
            role = Role.Tab,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    val icon: @Composable () -> Unit = {
        Box {
            AnimatedContent(
                modifier = Modifier.graphicsLayer {
                    scaleX = animatedIconScale
                    scaleY = animatedIconScale
                },
                targetState = selected,
                transitionSpec = {
                    (fadeIn() + scaleIn(initialScale = 0.88f))
                        .togetherWith(fadeOut() + scaleOut(targetScale = 1.06f))
                },
                label = "floating_tab_icon",
            ) { isSelected ->
                Icon(
                    painter = if (isSelected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = null,
                    tint = animatedContentColor,
                    modifier = Modifier
                        .size(TeakFloatingTabBarDefaults.TabIconSize)
                        .graphicsLayer {
                            val scale = item.iconScale.coerceIn(0.75f, 1.25f)
                            scaleX = scale
                            scaleY = scale
                        },
                )
            }
            item.badge?.let { label ->
                FloatingTabBadge(
                    label = label,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp),
                )
            }
        }
    }
    val label: @Composable () -> Unit = {
        Text(
            text = item.label,
            modifier = Modifier.teakDockLabel(layout),
            color = animatedContentColor,
            style = when (layout) {
                TeakFloatingTabLayout.Horizontal -> MaterialTheme.typography.labelMedium
                TeakFloatingTabLayout.Stacked -> MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                )
            },
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    when (layout) {
        TeakFloatingTabLayout.Horizontal -> Row(
            modifier = itemModifier.padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(5.dp))
            label()
        }

        TeakFloatingTabLayout.Stacked -> Column(
            modifier = itemModifier.padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            icon()
            label()
        }
    }
}

@Composable
private fun FloatingTabBadge(
    label: String,
    modifier: Modifier = Modifier,
) {
    val shape = TeakFloatingTabBarDefaults.IndicatorShape
    val colors = MaterialTheme.colorScheme
    if (label.isEmpty()) {
        Box(
            modifier = modifier
                .size(8.dp)
                .clip(shape)
                .background(colors.error),
        )
        return
    }

    Box(
        modifier = modifier
            .heightIn(min = 15.dp)
            .widthIn(min = 15.dp)
            .clip(shape)
            .background(colors.error)
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = colors.onError,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
    }
}
