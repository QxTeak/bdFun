package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.ProgressiveBlur
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.progressiveTextureBlur
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 页面 chrome 的统一尺寸 / 动画常量。
 *
 * 这些值决定了“大标题在内容里的固定位置”和“小标题收进顶栏后的固定位置”，
 * 所有使用 TeakCommonModule 的页面共享同一套视觉节奏。
 */
object TeakChromeDefaults {
    /** 顶栏内容高度（不含状态栏）。 */
    val TopBarHeight: Dp = 64.dp

    /** 页面内容的标准水平内边距。 */
    val ContentHorizontalPadding: Dp = 16.dp

    /** 页面内容的标准顶部 / 底部内边距。 */
    val ContentTopPadding: Dp = 8.dp
    val ContentBottomPadding: Dp = 40.dp

    /** 大标题上方留白：让大标题落在返回键下方。 */
    val LargeTitleTopSpacer: Dp = 52.dp

    /** 大标题 / 副标题相对内容区的左边距，以及两者间距。 */
    val LargeTitleStartPadding: Dp = 10.dp
    val SubtitleStartPadding: Dp = 12.dp
    val TitleSubtitleGap: Dp = 6.dp

    val LargeTitleFontSize = 30.sp
    val SmallTitleFontSize = 16.sp
    val SubtitleFontSize = 11.5.sp

    /** 顶部 overlay：返回键 / 操作按钮所在这一行的高度与内边距。 */
    val TopOverlayHeight: Dp = 56.dp
    val TopOverlayHorizontalPadding: Dp = 12.dp
    val TopOverlayVerticalPadding: Dp = 8.dp
    val TopOverlayItemSpacing: Dp = 8.dp

    /** 大标题开始折叠到完全消失时对应的滚动比例。 */
    const val CollapseFadeFraction = 0.65f

    /** 大标题折叠时的最大模糊半径。 */
    val CollapseBlurRadius: Dp = 12.dp

    /** 小标题在折叠进度超过该阈值后出现。 */
    const val SmallTitleThreshold = 0.35f

    /** 小标题出现动画固定 260ms，不随手指滚动速度变化。 */
    const val SmallTitleRevealDurationMs = 260f

    /** 小标题未完全出现时的最大模糊半径。 */
    val SmallTitleBlurRadius: Dp = 4.dp

    /** 小标题出现时从下方上移的距离。 */
    val SmallTitleRise: Dp = 7.dp

    /** 小标题相对顶栏视觉中心再上移的距离。 */
    val SmallTitleLift: Dp = 3.dp

    /** 顶栏背景渐变模糊参数。参数参考 HyperIsland，代码为 Teak 实现；记录见 docs/CREDITS.md。 */
    const val BarBlurRadius = 16f
    const val BarTintAlpha = 0.66f
    const val BarBlurStartFraction = 0.62f
    const val BarBlurEndFraction = 1f
    const val BarBlurCurve = 1.25f

    val BackButtonSize: Dp = 40.dp

    /** 未开启系统导航条时，底栏/视频页回复框额外抬高的距离。 */
    val RaisedBottomBarExtra: Dp = 16.dp
}

private val TeakBarProgressiveBlur = ProgressiveBlur.Top.copy(
    startFraction = TeakChromeDefaults.BarBlurStartFraction,
    endFraction = TeakChromeDefaults.BarBlurEndFraction,
    curve = TeakChromeDefaults.BarBlurCurve,
)

/**
 * 顶栏渐变模糊：顶栏底部清晰 -> 顶部完全模糊，底部用页面底色染色，
 * 因此顶栏和内容之间没有明显接缝。
 */
@Composable
fun Modifier.teakProgressiveBarBlur(
    color: Color = MiuixTheme.colorScheme.background,
    tintAlpha: Float = TeakChromeDefaults.BarTintAlpha,
    blurRadius: Float = TeakChromeDefaults.BarBlurRadius,
): Modifier {
    if (LocalProgressiveTopBarMaterialHosted.current) return this
    val backdrop = LocalTeakBackdrop.current
    val material = LocalTeakMaterialSpec.current
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(BlendColorEntry(color = color.copy(alpha = tintAlpha))),
    )
    return if (backdrop != null && material.textureBlurEnabled) {
        progressiveTextureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = blurRadius,
            gradient = TeakBarProgressiveBlur,
            noiseCoefficient = 0f,
            colors = blurColors,
        )
    } else {
        background(color, RectangleShape)
    }
}

/**
 * 固定顶栏：渐变模糊背景 + 居中小标题。
 *
 * 小标题只在 [collapse] 超过 [TeakChromeDefaults.SmallTitleThreshold] 后出现，
 * 且始终用固定的 260ms 补间（淡入 + 去模糊 + 微量上移），不会因为手指滚动速度
 * 而忽快忽慢。
 */
@Composable
fun TeakTopBar(
    title: String,
    collapse: Float,
    modifier: Modifier = Modifier,
    barHeight: Dp = TeakChromeDefaults.TopBarHeight,
    titleThreshold: Float = TeakChromeDefaults.SmallTitleThreshold,
    revealDurationMs: Float = TeakChromeDefaults.SmallTitleRevealDurationMs,
    /** 没有大标题的页面（如私信）强制常显小标题。 */
    titleAlwaysVisible: Boolean = false,
    /** 顶栏渐变模糊的显隐：0 = 完全透明，1 = 正常显示。 */
    barBlurFraction: Float = 1f,
    /** 聊天等没有折叠标题的页面可固定顶栏材质强度。 */
    topBarMaterialProgress: Float? = null,
) {
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val animatedShown = rememberManualProgress(
        target = if (collapse > titleThreshold) 1f else 0f,
        durationMs = revealDurationMs,
    )
    val shown = if (titleAlwaysVisible) 1f else animatedShown
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(barHeight + statusBarTop),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { alpha = barBlurFraction.coerceIn(0f, 1f) }
                .teakProgressiveBarBlur(),
        )
        if (shown > 0.01f) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = TeakChromeDefaults.SmallTitleFontSize,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = statusBarTop)
                    .offset(y = -TeakChromeDefaults.SmallTitleLift)
                    .graphicsLayer {
                        alpha = shown
                        translationY = (1f - shown) * TeakChromeDefaults.SmallTitleRise.toPx()
                    }
                    .blur(TeakChromeDefaults.SmallTitleBlurRadius * (1f - shown)),
            )
        }
    }
}

/**
 * 滚动大标题：固定在内容顶部的固定位置（返回键下方 52dp），
 * 随 [TeakPageScrollState.collapse] 淡出并模糊。
 */
@Composable
fun TeakLargeTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    topSpacer: Dp = TeakChromeDefaults.LargeTitleTopSpacer,
    /** 头图上叠标题时传白色；普通页面默认跟随主题。 */
    titleColor: Color = MiuixTheme.colorScheme.onBackground,
    subtitleColor: Color = MiuixTheme.colorScheme.onBackgroundVariant,
) {
    if (isWideFoldTopBar()) { Spacer(Modifier.height(TeakChromeDefaults.TopOverlayHeight - 12.dp)); return }
    val colors = MiuixTheme.colorScheme
    val p = (pageState.collapse / TeakChromeDefaults.CollapseFadeFraction).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = 1f - p }
            .blur(TeakChromeDefaults.CollapseBlurRadius * p),
    ) {
        Spacer(Modifier.height(topSpacer))
        Text(
            text = title,
            modifier = Modifier.padding(start = TeakChromeDefaults.LargeTitleStartPadding),
            color = titleColor,
            fontSize = TeakChromeDefaults.LargeTitleFontSize,
            fontWeight = FontWeight.Normal,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(TeakChromeDefaults.TitleSubtitleGap))
            Text(
                text = subtitle,
                modifier = Modifier.padding(start = TeakChromeDefaults.SubtitleStartPadding),
                color = subtitleColor,
                fontSize = TeakChromeDefaults.SubtitleFontSize,
            )
        }
    }
}

/**
 * 通用页面外壳：主题外部注入，这里负责
 * 1. 建立玻璃 backdrop 采样层；
 * 2. 固定顶栏的渐变模糊与小标题；
 * 3. 左上角玻璃返回键；
 * 4. 独立的 overlay 层（可安全读取 backdrop 做真模糊）。
 *
 * [content] 在采集层内；[overlay] 与返回键在采集层外。
 */
@Composable
fun TeakPageScaffold(
    title: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    pageState: TeakPageScrollState = LocalTeakPageScrollState.current,
    backRevealed: Boolean? = null,
    showBackButton: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    contentModifier: Modifier = Modifier,
    limitContentWidth: Boolean = true,
    showTopBar: Boolean = true,
    topBarLeading: @Composable RowScope.() -> Unit = {},
    topBarCenter: @Composable BoxScope.() -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    /** 顶栏三个图标的自定义颜色；null 时跟随主题。 */
    topBarGlyphColor: Color? = null,
    overlay: @Composable BoxScope.() -> Unit = {},
    /** 画在顶栏按钮之上的浮层，给需要盖住顶栏的玻璃菜单用。 */
    menuOverlay: @Composable BoxScope.() -> Unit = {},
    titleAlwaysVisible: Boolean = false,
    /** 顶栏渐变模糊的显隐：0 = 完全透明，1 = 正常显示。 */
    barBlurFraction: Float = 1f,
    /** 聊天等没有折叠标题的页面可固定顶栏材质强度。 */
    topBarMaterialProgress: Float? = null,
    content: @Composable () -> Unit,
) {
    val wide = isWideFoldTopBar()
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    com.aritxonly.teak.common.modal.RegisterAdvancedMaterialModalBackdrop(backdrop)
    CompositionLocalProvider(LocalTeakBackdrop provides backdrop, LocalTeakPageScrollState provides pageState) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
                    .then(contentModifier),
            ) {
                if (title != null && limitContentWidth) {
                    TeakScrollContainer(pageState.scrollableState, Modifier.fillMaxSize(), content = content)
                } else content()
            }
            Box(modifier = Modifier.fillMaxSize()) { overlay() }
            if (showTopBar) CompositionLocalProvider(LocalTopBarScrollProgress provides ((topBarMaterialProgress ?: pageState.materialProgress).coerceIn(0f, 1f) * barBlurFraction.coerceIn(0f, 1f))) {
                ProgressiveTopBarMaterial {
                    if (title != null && !wide) {
                        TeakTopBar(
                            title = title,
                            collapse = pageState.collapse,
                            titleAlwaysVisible = titleAlwaysVisible,
                        )
                    }
                    if (wide && title != null) {
                        WideFoldTopAppBar(
                            title = title,
                            titleColor = topBarGlyphColor ?: MiuixTheme.colorScheme.onSurface,
                            navigationIcon = {
                                androidx.compose.foundation.layout.Row {
                                    if (showBackButton) TeakGlassBackButton(onClick = onBack, glyphColor = topBarGlyphColor)
                                    topBarLeading()
                                }
                            },
                            actions = {
                                Box { topBarCenter() }
                                topBarActions()
                            },
                            bottomContent = {},
                        )
                    } else {
                        TeakTopOverlay(
                            revealed = backRevealed ?: pageState.backRevealed,
                            onBack = if (showBackButton) onBack else null,
                            light = backRevealed == false,
                            glyphColor = topBarGlyphColor,
                            leading = topBarLeading,
                            center = topBarCenter,
                            trailing = topBarActions,
                        )
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                menuOverlay()
            }
        }
    }
}
