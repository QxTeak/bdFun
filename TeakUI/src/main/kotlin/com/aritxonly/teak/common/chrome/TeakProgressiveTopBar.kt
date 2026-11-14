package com.aritxonly.teak.common.chrome

import com.aritxonly.teak.common.material.*

import androidx.compose.runtime.getValue
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import kotlin.math.ceil
import kotlin.math.max

val LocalImmersiveTopBarEnabled = staticCompositionLocalOf { false }

val LocalProgressiveTopBarMaterialHosted = staticCompositionLocalOf { false }
val LocalTopBarScrollProgress = staticCompositionLocalOf<Float?> { null }
val LocalTopBarButtonMaterialProgress = staticCompositionLocalOf { 0f }

internal const val ProgressiveTopBarBlurRadiusDp = TeakChromeDefaults.BarBlurRadius
internal const val ProgressiveTopBarBlurFadeStart = TeakChromeDefaults.BarBlurStartFraction
internal const val ProgressiveTopBarOverlayTopAlpha = TeakChromeDefaults.BarTintAlpha
internal const val ProgressiveTopBarOverlayEndFraction = 0.80f
internal const val ProgressiveTopBarContentEndFraction = 0.75f
internal const val ProgressiveTopBarMinimumContentHeightDp = 56f
internal const val ProgressiveTopBarMaterialHeightTrimDp = 6f

/** Extra visual clearance below the fully transparent end of the progressive material. */
internal const val ProgressiveTopBarBottomClearanceDp = 12f

internal data class ProgressiveTopBarHeightMetrics(
    val occupiedHeightPx: Int,
    val materialHeightPx: Int,
)

/** Keeps progressive blur in lockstep with the scroll-driven soft-glass reveal. */
internal fun resolveProgressiveTopBarMaterialProgress(scrollProgress: Float?): Float =
    scrollProgress?.coerceIn(0f, 1f) ?: 0f

internal fun resolveProgressiveTopBarHeightMetrics(
    measuredTopBarHeightPx: Int,
    statusBarHeightPx: Int,
    minimumContentHeightPx: Int,
    contentEndFraction: Float = ProgressiveTopBarContentEndFraction,
    materialHeightTrimPx: Int = 0,
): ProgressiveTopBarHeightMetrics {
    if (measuredTopBarHeightPx <= 0) {
        return ProgressiveTopBarHeightMetrics(
            occupiedHeightPx = 0,
            materialHeightPx = 0,
        )
    }
    val occupiedHeight = max(
        measuredTopBarHeightPx.coerceAtLeast(0),
        statusBarHeightPx.coerceAtLeast(0) + minimumContentHeightPx.coerceAtLeast(0),
    )
    val safeContentEndFraction = contentEndFraction.coerceIn(0.01f, 1f)
    val untrimmedMaterialHeight = ceil(occupiedHeight / safeContentEndFraction).toInt()
    return ProgressiveTopBarHeightMetrics(
        occupiedHeightPx = occupiedHeight,
        materialHeightPx = (untrimmedMaterialHeight - materialHeightTrimPx.coerceAtLeast(0))
            .coerceAtLeast(occupiedHeight),
    )
}

/**
 * Draws a Miuix progressive backdrop beneath the top-bar foreground. When advanced material is
 * disabled, the existing transparent gradient remains as the intentional lightweight fallback.
 */
@Composable
fun ProgressiveTopBarMaterial(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    /**
     * 底层页面正在做一镜到底时，内容被整体上移了多少（px）。
     *
     * backdrop 录的是变换之前的画面，而顶栏画在变换之外；不补偿的话，
     * 顶栏采样到的是"还没上移"的内容，接缝就出现在两者交界处。
     * 这里把采样区域的布局位置下移同样的距离，再用绘制位移抵消回来：
     * 画面不动，取景跟着内容走。
     */
    underlayLiftPx: Float = 0f,
    /** 内容自身叠加的模糊半径（px）；顶栏要补上同样的量，否则两边清晰度不一致。 */
    underlayBlurPx: Float = 0f,
    /** 位于播放器下方的顶栏不占用状态栏，不应重复补足该高度。 */
    includeStatusBar: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) { Box(modifier, content = content); return }
    val materialTarget = resolveProgressiveTopBarMaterialProgress(LocalTopBarScrollProgress.current)
    val materialProgress by animateFloatAsState(
        targetValue = materialTarget,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "progressive-top-bar-material",
    )
    val density = LocalDensity.current
    val statusBarHeightPx = if (includeStatusBar) WindowInsets.statusBars.getTop(density) else 0
    val minimumContentHeightPx = with(density) {
        ProgressiveTopBarMinimumContentHeightDp.dp.roundToPx()
    }
    val materialHeightTrimPx = with(density) {
        (ProgressiveTopBarMaterialHeightTrimDp + ProgressiveTopBarBottomClearanceDp)
            .dp
            .roundToPx()
    }

    SubcomposeLayout(modifier = modifier) { constraints ->
        val contentPlaceables = subcompose(ProgressiveTopBarSlot.Content) {
            CompositionLocalProvider(
                LocalProgressiveTopBarMaterialHosted provides true,
                LocalTopBarButtonMaterialProgress provides materialProgress,
            ) {
                // Include custom and raw MIUIX headers, outside their clipping/transition layers.
                // This foreground is drawn after the progressive backdrop, never into its recorder.
                TopBarGlassShadowHost {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        content = content,
                    )
                }
            }
        }.map { measurable ->
            measurable.measure(constraints.copy(minHeight = 0))
        }
        val contentWidth = contentPlaceables.maxOfOrNull { it.width } ?: constraints.minWidth
        val contentHeight = contentPlaceables.maxOfOrNull { it.height } ?: constraints.minHeight
        val width = contentWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentHeight.coerceIn(constraints.minHeight, constraints.maxHeight)
        val heightMetrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = height,
            statusBarHeightPx = statusBarHeightPx,
            minimumContentHeightPx = minimumContentHeightPx,
            materialHeightTrimPx = materialHeightTrimPx,
        )
        val materialConstraints = Constraints.fixed(
            width = width,
            height = heightMetrics.materialHeightPx,
        )
        val backgroundPlaceables = subcompose(ProgressiveTopBarSlot.Background) {
            ProgressiveTopBarBackground(
                materialProgress = materialProgress,
                underlayLiftPx = underlayLiftPx,
                underlayBlurPx = underlayBlurPx,
                modifier = Modifier.fillMaxSize(),
            )
        }.map { measurable -> measurable.measure(materialConstraints) }

        layout(width, height) {
            backgroundPlaceables.forEach { it.placeRelative(0, 0) }
            contentPlaceables.forEach { it.placeRelative(0, 0) }
        }
    }
}

@Composable
private fun ProgressiveTopBarBackground(
    materialProgress: Float,
    underlayLiftPx: Float = 0f,
    underlayBlurPx: Float = 0f,
    modifier: Modifier = Modifier,
) {
    if (materialProgress <= 0f) return

    val density = LocalDensity.current
    val lift = underlayLiftPx.coerceAtLeast(0f)
    val shiftDp = with(density) { lift.toDp() }
    val extraBlurDp = with(density) { underlayBlurPx.coerceAtLeast(0f).toDp().value }
    val blurRadius = ProgressiveTopBarBlurRadiusDp + extraBlurDp
    // 布局下移、绘制上移，净位移为 0；被移动的只是"取景框"。
    val samplingShift = Modifier
        .offset(y = shiftDp)
        .graphicsLayer { translationY = -lift }

    val surface = MaterialTheme.colorScheme.surface
    val advancedMaterial = LocalTeakMaterialSpec.current
    val backdrop = LocalTeakBackdrop.current
    if (advancedMaterial.useOfficialMiuixGlass && advancedMaterial.textureBlurEnabled &&
        backdrop != null && top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported()) {
        // Keep the app's edge fade and scroll ownership, with the official action-bar body.
        Box(modifier.then(samplingShift).graphicsLayer {
            alpha = materialProgress
            compositingStrategy = CompositingStrategy.Offscreen
        }.drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.White,
                    ProgressiveTopBarBlurFadeStart to Color.White,
                    1f to Color.Transparent,
                ),
                blendMode = BlendMode.DstIn,
            )
        }.officialMiuixGlassMaterial(
            backdrop = backdrop,
            shape = RectangleShape,
            isDark = MaterialTheme.colorScheme.onSurface.luminance() > 0.5f,
            role = GlassSurfaceRole.TopBar,
        ))
        return
    }
    Box(
        modifier = modifier.graphicsLayer(alpha = materialProgress),
    ) {
        if (advancedMaterial.enabled && backdrop != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(if (lift > 0f) samplingShift else Modifier)
                    .progressiveTopBarBackdrop(
                        backdrop = requireNotNull(backdrop),
                        blurRadius = blurRadius,
                    ),
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to surface.copy(alpha = ProgressiveTopBarOverlayTopAlpha),
                            ProgressiveTopBarOverlayEndFraction to Color.Transparent,
                            1f to Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}

private enum class ProgressiveTopBarSlot {
    Background,
    Content,
}

private fun Modifier.progressiveTopBarBackdrop(
    backdrop: Backdrop,
    blurRadius: Float = ProgressiveTopBarBlurRadiusDp,
): Modifier = progressiveTextureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    blurRadius = blurRadius,
    gradient = ProgressiveBlur.Top.copy(
        startFraction = ProgressiveTopBarBlurFadeStart,
        endFraction = TeakChromeDefaults.BarBlurEndFraction,
        curve = TeakChromeDefaults.BarBlurCurve,
    ),
)
