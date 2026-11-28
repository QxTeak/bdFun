package com.aritxonly.teak.common.chrome

import com.aritxonly.teak.common.material.*

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import kotlin.math.ceil
import kotlin.math.max

val LocalProgressiveTopBarMaterialHosted = staticCompositionLocalOf { false }
val LocalTopBarScrollProgress = staticCompositionLocalOf<Float?> { null }

/**
 * 顶栏按钮的材质进度。
 *
 * 这里故意暴露 `State<Float>` 而不是 Float：滚动渐显只应在 draw 阶段读取，
 * 不能因为进度变化让整个顶栏重新组合、重新测量。按钮/阴影请用
 * `graphicsLayer { alpha = state.value }` 这类绘制期读法。
 */
val LocalTopBarButtonMaterialProgress = staticCompositionLocalOf<State<Float>> {
    mutableStateOf(0f)
}

/**
 * 顶栏按钮/阴影的 draw 期 alpha 提供器。
 *
 * 不在顶栏材质宿主内时返回 null，调用方继续使用静态 [materialAlpha]；
 * 在宿主内时返回读取 [LocalTopBarButtonMaterialProgress] 的 lambda，
 * 让进度只在绘制阶段生效。
 */
@Composable
fun topBarMaterialAlphaProvider(): (() -> Float)? {
    if (!LocalProgressiveTopBarMaterialHosted.current) return null
    val state = LocalTopBarButtonMaterialProgress.current
    return remember(state) { { state.value.coerceIn(0f, 1f) } }
}

internal const val ProgressiveTopBarBlurRadiusDp = TeakChromeDefaults.BarBlurRadius
internal const val ProgressiveTopBarBlurFadeStart = TeakChromeDefaults.BarBlurStartFraction
internal const val ProgressiveTopBarOverlayTopAlpha = TeakChromeDefaults.BarTintAlpha
internal const val ProgressiveTopBarOverlayEndFraction = 0.80f
private const val ProgressiveTopBarEdgeFadeStart = 0.88f
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
 *
 * 实现要点：内容只组合、测量一次；顶部渐显由 [State] 在 draw 阶段读取。
 * 这样滚动渐显期间不会重新 subcompose，也不会重新测量顶栏（对齐 MIUIX GlassTopAppBar 的结构）。
 */
@Composable
fun ProgressiveTopBarMaterial(
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    /** 位于播放器下方的顶栏不占用状态栏，不应重复补足该高度。 */
    includeStatusBar: Boolean = true,
    /**
     * 材质实际绘制高度（px），测量完成后回调一次。
     *
     * 材质比顶栏内容高三分之一，是为了让底部那段模糊慢慢收尾；但调用方常在外层套
     * `graphicsLayer { alpha < 1 }`（淡出、换页、进 UP 页）。HWUI 渲染半透明层时按
     * **外层节点边界**裁剪，裁口正好落在渐变还有强度的地方，滚动的画面穿过它就成了
     * 一条硬边。调用方拿这个高度把半透明层撑到材质那么高，裁口就落到渐变已经为 0 的
     * 位置：观感与裁剪无关，配方（半径/染色/起止）一个参数都不动。
     */
    onMaterialHeightPx: ((Int) -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    if (!enabled) { Box(modifier, content = content); return }
    val materialTarget = resolveProgressiveTopBarMaterialProgress(LocalTopBarScrollProgress.current)
    val materialProgress = animateFloatAsState(
        targetValue = materialTarget,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "progressive-top-bar-material",
    )
    // 只在"从无到有/从有到无"时切换背景槽，进度本身不进入组合期读取。
    val materialVisible by remember(materialProgress) {
        derivedStateOf { materialProgress.value > 0.001f }
    }
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
    // 只在高度真的变了才回调：调用方据此调整半透明层高度，重复上报会让测量来回跳。
    val reportedMaterialHeightPx = remember { intArrayOf(-1) }

    Layout(
        modifier = modifier,
        content = {
            if (materialVisible) {
                ProgressiveTopBarBackground(
                    materialProgress = materialProgress,
                    modifier = Modifier.fillMaxSize(),
                )
            }
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
        },
    ) { measurables, constraints ->
        val contentIndex = if (materialVisible) 1 else 0
        val contentPlaceable = measurables[contentIndex].measure(constraints.copy(minHeight = 0))
        val width = contentPlaceable.width.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = contentPlaceable.height.coerceIn(constraints.minHeight, constraints.maxHeight)
        val heightMetrics = resolveProgressiveTopBarHeightMetrics(
            measuredTopBarHeightPx = height,
            statusBarHeightPx = statusBarHeightPx,
            minimumContentHeightPx = minimumContentHeightPx,
            materialHeightTrimPx = materialHeightTrimPx,
        )
        // 上报放在测量之后：高度由内容决定，调用方只是把外层半透明层撑高，不会反过来改内容。
        if (onMaterialHeightPx != null &&
            reportedMaterialHeightPx[0] != heightMetrics.materialHeightPx
        ) {
            reportedMaterialHeightPx[0] = heightMetrics.materialHeightPx
            onMaterialHeightPx(heightMetrics.materialHeightPx)
        }
        val backgroundPlaceable = if (materialVisible) {
            measurables[0].measure(
                Constraints.fixed(
                    width = width,
                    height = heightMetrics.materialHeightPx,
                ),
            )
        } else {
            null
        }
        layout(width, height) {
            backgroundPlaceable?.placeRelative(0, 0)
            contentPlaceable.placeRelative(0, 0)
        }
    }
}

@Composable
private fun ProgressiveTopBarBackground(
    materialProgress: State<Float>,
    modifier: Modifier = Modifier,
) {
    val surface = MaterialTheme.colorScheme.surface
    val advancedMaterial = LocalTeakMaterialSpec.current
    val backdrop = LocalTeakBackdrop.current
    if (advancedMaterial.useOfficialMiuixGlass && advancedMaterial.textureBlurEnabled &&
        backdrop != null && top.yukonga.miuix.kmp.material.isRuntimeShaderSupported()) {
        // Keep the app's edge fade and scroll ownership, with the official action-bar body.
        Box(modifier.graphicsLayer {
            alpha = materialProgress.value.coerceIn(0f, 1f)
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
    val usesBackdrop = advancedMaterial.enabled && backdrop != null
    val materialLayer = if (usesBackdrop) Modifier.progressiveTopBarEdgeLayer(materialProgress)
        else Modifier.graphicsLayer { alpha = materialProgress.value.coerceIn(0f, 1f) }
    Box(modifier.then(materialLayer)) {
        if (usesBackdrop) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .progressiveTopBarBackdrop(requireNotNull(backdrop)),
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

private fun Modifier.progressiveTopBarBackdrop(backdrop: Backdrop): Modifier = progressiveTextureBlur(
    backdrop = backdrop,
    shape = RectangleShape,
    blurRadius = ProgressiveTopBarBlurRadiusDp,
    gradient = TeakBarProgressiveBlur,
)

/** 模糊半径归零时采样层依然在绘制。让末端的采样像素逐渐交还给底下的正文。 */
internal fun Modifier.progressiveTopBarEdgeLayer(progress: State<Float>): Modifier =
    graphicsLayer {
        alpha = progress.value.coerceIn(0f, 1f)
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val tail = Brush.verticalGradient(
            ProgressiveTopBarEdgeFadeStart to Color.White,
            1f to Color.Transparent,
        )
        onDrawWithContent {
            drawContent()
            drawRect(brush = tail, blendMode = BlendMode.DstIn)
        }
    }
