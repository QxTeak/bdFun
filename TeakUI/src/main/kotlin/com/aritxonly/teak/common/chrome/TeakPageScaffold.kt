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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import com.aritxonly.teak.common.interaction.blockTouches
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import com.aritxonly.teak.common.search.teakSearchUnderlay
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.luminance
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
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.material.GlassDefaults
import top.yukonga.miuix.kmp.material.GlassShape
import top.yukonga.miuix.kmp.glass.glassPanel
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 只有页面级 chrome 才会把大标题换成顶栏占位，引导页等内容页不受影响。 */
internal val LocalTeakClassicChrome = compositionLocalOf { false }

/** 紧凑竖屏（手机）：尺寸向 Miuix 靠拢；宽屏/折叠保持现有布局。 */
@Composable
internal fun isCompactPortraitTopBar(): Boolean {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    return configuration.smallestScreenWidthDp < 600 &&
        configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
}

/** 顶栏动作圆钮尺寸：紧凑竖屏与返回键同为 44dp，宽屏保持 40dp。 */
@Composable
fun teakTopBarActionSize(): Dp = if (isCompactPortraitTopBar()) {
    TeakChromeDefaults.CompactBackButtonSize
} else {
    40.dp
}


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

    /** 紧凑竖屏（手机）按 Miuix 官方顶栏的字号；宽屏/折叠仍用上面那组。 */
    val CompactLargeTitleFontSize = 32.sp
    val CompactSmallTitleFontSize = 18.sp
    val CompactSubtitleFontSize = 13.sp
    val CompactBackButtonSize: Dp = 44.dp

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

/** 搜索页顶栏从下方抬进画面的距离：搜索结果面板本身就是从下往上，顶栏跟着同一条方向。 */
private val SearchedChromeRise = 14.dp

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
 * 经典样式的顶栏底：用 Miuix 自带的玻璃材质整块模糊，不做渐变；
 * 关闭高级材质时退化为平面底色，也不再画分割线。
 */
@Composable
fun Modifier.teakClassicBarGlass(): Modifier {
    val material = LocalTeakMaterialSpec.current
    val backdrop = LocalTeakBackdrop.current
    val colors = MiuixTheme.colorScheme
    val tint = colors.surfaceContainer
    val blurColors = BlurDefaults.blurColors(
        blendColors = listOf(BlendColorEntry(color = tint.copy(alpha = 0.35f))),
    )
    return if (backdrop != null && material.enabled && material.blurEnabled) {
        // Miuix 的整块纹理模糊：整个栏统一模糊，不做上下渐变。
        textureBlur(
            backdrop = backdrop,
            shape = RectangleShape,
            blurRadius = TeakChromeDefaults.BarBlurRadius,
            noiseCoefficient = 0f,
            colors = blurColors,
        )
    } else {
        background(tint)
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
        val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
        if (classic) {
            // 经典：顶栏常驻，整条用 Miuix 玻璃模糊（关闭高级材质时自动退化为平面）。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .teakClassicBarGlass(),
            )
            // 玻璃上再压一层半透明底色，保证标题和图标在内容滚过时仍然清晰。
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MiuixTheme.colorScheme.surfaceContainer.copy(alpha = 0.62f)),
            )
            SmallTitleText(title, statusBarTop, 1f)
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { alpha = barBlurFraction.coerceIn(0f, 1f) }
                    .teakProgressiveBarBlur(),
            )
            SmallTitleText(title, statusBarTop, shown)
        }
    }
}

/** 顶栏居中小标题；[reveal] 同时控制淡入与上移，0 时不参与布局/绘制。 */
@Composable
private fun BoxScope.SmallTitleText(text: String, statusBarTop: Dp, reveal: Float) {
    val shown = reveal.coerceIn(0f, 1f)
    if (shown <= 0.001f) return
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = if (isCompactPortraitTopBar()) TeakChromeDefaults.CompactSmallTitleFontSize else TeakChromeDefaults.SmallTitleFontSize,
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
    /** 由 [TeakPinnedLargeTitleHost] 画的那一份：不参与“列表副本隐藏”的判断。 */
    drawnAsPinnedCopy: Boolean = false,
) {
    // 经典页面不再在内容里画大标题，标题常驻顶栏；这里只保留顶栏占位，让内容从栏下开始。
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic && LocalTeakClassicChrome.current) {
        Spacer(Modifier.height(56.dp))
        return
    }
    if (isWideFoldTopBar()) { Spacer(Modifier.height(TeakChromeDefaults.TopOverlayHeight - 12.dp)); return }
    // 页面外壳会把标题复制到滚动容器外固定绘制；列表里这一份只占位并登记文案。
    val anchorState = LocalTeakLargeTitleAnchor.current
    val hosted = anchorState != null && !drawnAsPinnedCopy
    if (anchorState != null && hosted) {
        anchorState.data = TeakLargeTitleData(title, subtitle, titleColor, subtitleColor)
        DisposableEffect(anchorState) {
            anchorState.register()
            onDispose { anchorState.unregister() }
        }
    }
    TeakLargeTitleBody(
        title = title,
        subtitle = subtitle,
        titleColor = titleColor,
        subtitleColor = subtitleColor,
        pageState = pageState,
        topSpacer = topSpacer,
        modifier = modifier,
        hidden = hosted,
    )
}

/**
 * 大标题本体：折叠淡出与折叠模糊都在各自的绘制层里读取，不随滚动重组，
 * 也不会被顶栏 backdrop 二次采样。
 */
@Composable
private fun TeakLargeTitleBody(
    title: String,
    subtitle: String?,
    titleColor: Color,
    subtitleColor: Color,
    pageState: TeakPageScrollState,
    topSpacer: Dp,
    modifier: Modifier,
    hidden: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val p = (pageState.collapse / TeakChromeDefaults.CollapseFadeFraction)
                    .coerceIn(0f, 1f)
                alpha = if (hidden) 0f else 1f - p
                renderEffect = if (!hidden && p > 0.001f) {
                    val radius = TeakChromeDefaults.CollapseBlurRadius.toPx() * p
                    BlurEffect(radius, radius, TileMode.Clamp)
                } else {
                    null
                }
            },
    ) {
        Spacer(Modifier.height(topSpacer))
        Text(
            text = title,
            modifier = Modifier.padding(start = TeakChromeDefaults.LargeTitleStartPadding),
            color = titleColor,
            fontSize = if (isCompactPortraitTopBar()) TeakChromeDefaults.CompactLargeTitleFontSize else TeakChromeDefaults.LargeTitleFontSize,
            fontWeight = FontWeight.Normal,
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(Modifier.height(TeakChromeDefaults.TitleSubtitleGap))
            Text(
                text = subtitle,
                modifier = Modifier.padding(start = TeakChromeDefaults.SubtitleStartPadding),
                color = subtitleColor,
                fontSize = if (isCompactPortraitTopBar()) TeakChromeDefaults.CompactSubtitleFontSize else TeakChromeDefaults.SubtitleFontSize,
            )
        }
    }
}

/**
 * 顶栏前景里的大标题：与 MIUIX 一致，标题属于顶栏前景而不是页面录制层，
 * 因此只会被自己的折叠模糊处理一次，不会被顶栏的 progressive blur 二次采样。
 *
 * 位移、折叠进度、以及页面自己登记的一镜到底退让（[TeakLargeTitleSearchUnderlay]）
 * 全部在绘制阶段读取：滚动 / 搜索动画期间只失效绘制层，不重组。
 */
@Composable
private fun TeakLargeTitleForeground(
    state: TeakLargeTitleAnchorState,
    pageState: TeakPageScrollState,
    topInset: Dp,
    underlayLiftPx: Float,
    underlayAlpha: Float,
    underlayBlurProgress: Float,
) {
    if (state.present <= 0) return
    val data = state.data ?: return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val motion = state.searchUnderlayProgress.floatValue
                val lift = TeakSearchTransitionDefaults.ContentLift.toPx() * motion
                translationY = -currentScrollOffsetPx(pageState) - underlayLiftPx - lift
                alpha = underlayAlpha * state.searchUnderlayAlpha.floatValue
                val blurProgress = maxOf(
                    underlayBlurProgress,
                    state.searchUnderlayBlur.floatValue,
                )
                renderEffect = if (blurProgress > 0.001f) {
                    val radius = TeakSearchTransitionDefaults.UnderlayBlurPx * blurProgress
                    BlurEffect(radius, radius, TileMode.Clamp)
                } else {
                    null
                }
            },
    ) {
        TeakLargeTitleBody(
            title = data.title,
            subtitle = data.subtitle,
            titleColor = data.titleColor,
            subtitleColor = data.subtitleColor,
            pageState = pageState,
            topSpacer = TeakChromeDefaults.LargeTitleTopSpacer,
            modifier = Modifier.padding(
                start = TeakChromeDefaults.ContentHorizontalPadding,
                end = TeakChromeDefaults.ContentHorizontalPadding,
                top = topInset,
            ),
            hidden = false,
        )
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
    /** 最顶层全屏覆盖，画在顶栏之后；媒体库搜索等整页转场用。 */
    topOverlay: @Composable BoxScope.() -> Unit = {},
    /**
     * 整页搜索的「一镜到底」：0 = 正常，1 = 页面已退到底。
     *
     * 只作用在内容采集层（含吸顶大标题），overlay / 顶栏不在其中：
     * 顶栏按 [topBarAlpha] 单独淡出，并用同样的位移和模糊补偿采样。
     */
    underlayProgress: Float = 0f,
    underlayAlpha: Float = 1f,
    underlayBlurProgress: Float = 0f,
    /** 顶栏整体透明度；一镜到底期间和内容同步淡出。 */
    topBarAlpha: Float = 1f,
    titleAlwaysVisible: Boolean = false,
    /** 顶栏渐变模糊的显隐：0 = 完全透明，1 = 正常显示。 */
    barBlurFraction: Float = 1f,
    /** 聊天等没有折叠标题的页面可固定顶栏材质强度。 */
    topBarMaterialProgress: Float? = null,
    /**
     * 搜索覆盖层自己的顶栏文案（可以为空串：搜索页没有标题，只留返回键）。
     *
     * 非 null 时外壳会多画一份"搜索页 chrome"：旧页顶栏缩进淡出，这一份从下方抬进来。
     * 不这么做的话，开到一半的页面小标题会先换成搜索标题再消失。
     */
    searchTitle: String? = null,
    /** 搜索页 chrome 是否已接管（1 = 已落定）：只在落定后读折叠进度，避免进场闪一下小标题。 */
    searchTitleProgress: Float = 0f,
    content: @Composable () -> Unit,
) {
    val wide = isWideFoldTopBar()
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    com.aritxonly.teak.common.modal.RegisterAdvancedMaterialModalBackdrop(backdrop)
    val largeTitleState = remember { TeakLargeTitleAnchorState() }
    val largeTitleTopInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() +
        TeakChromeDefaults.ContentTopPadding
    CompositionLocalProvider(
        LocalTeakBackdrop provides backdrop,
        LocalTeakPageScrollState provides pageState,
        LocalTeakClassicChrome provides com.aritxonly.teak.common.theme.TeakUiStyle.isClassic,
        LocalTeakLargeTitleAnchor provides largeTitleState,
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .teakSearchUnderlay(underlayProgress, underlayAlpha, underlayBlurProgress)
                    .then(if (underlayProgress > 0.001f) Modifier.blockTouches(true) else Modifier),
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
                    // 大标题已移到顶栏前景层绘制（不进入 backdrop 录制），避免被顶栏渐进模糊二次模糊。
                }
            }
            Box(modifier = Modifier.fillMaxSize()) { overlay() }
            // 点顶栏回顶：常开。画在顶栏按钮之前（下面），空白处才吃掉点击。
            TeakStatusBarScrollToTop(
                pageState = pageState,
                belowStatusBar = if (showTopBar && !wide) TeakChromeDefaults.TopBarHeight else 0.dp,
            )
            // 顶栏始终留在组合里：一镜到底时只改透明度，避免动画中途卸载/重建整棵顶栏
            // 造成一帧跳动；搜索层画在 topOverlay，盖在顶栏之上，透明时也接不到点击。
            val underlayLiftPx = with(androidx.compose.ui.platform.LocalDensity.current) {
                TeakSearchTransitionDefaults.ContentLift.toPx() *
                    underlayProgress.coerceIn(0f, 1f)
            }
            // 搜索退让进度：外壳自己的参数，加上页面在内容里登记的那一份（设置页搜索）。
            // 顶栏整行（返回键 / 小标题 / 操作按钮）和首页一致地向内缩并淡出，
            // 搜索层自己的顶栏按同一进度淡入，返回键才不会凭空消失。
            fun Modifier.searchChromeMotion(incoming: Boolean) = graphicsLayer {
                val p = maxOf(
                    underlayProgress,
                    largeTitleState.searchUnderlayProgress.floatValue,
                ).coerceIn(0f, 1f)
                // 旧的 chrome 向内缩并淡出；搜索页是**新的一层**，跟着搜索结果一起从下往上抬进来，
                // 而不是从中心放大（那样看起来像是同一个返回键被拉伸）。
                if (incoming) {
                    scaleX = 1f
                    scaleY = 1f
                    translationY = SearchedChromeRise.toPx() * (1f - p)
                } else {
                    val scale = 1f - 0.1f * p
                    scaleX = scale
                    scaleY = scale
                    translationY = 0f
                }
                transformOrigin = TransformOrigin(0.5f, 0.35f)
                // 两段 chrome 都在时不要出现"两个返回键"：旧的一份先退干净，新的一份后半个身位才起。
                alpha = if (incoming) p * p else (1f - p) * (1f - p)
            }
            if (showTopBar) CompositionLocalProvider(LocalTopBarScrollProgress provides ((topBarMaterialProgress ?: pageState.materialProgress).coerceIn(0f, 1f) * barBlurFraction.coerceIn(0f, 1f))) {
                // 只有做一镜到底时才开独立图层，普通页面保持原来的绘制路径。
                val topBarLayer = if (topBarAlpha < 0.999f) {
                    Modifier.graphicsLayer { alpha = topBarAlpha.coerceIn(0f, 1f) }
                } else {
                    Modifier
                }
                ProgressiveTopBarMaterial(
                    modifier = topBarLayer,
                    underlayLiftPx = underlayLiftPx,
                    underlayBlurPx = TeakSearchTransitionDefaults.UnderlayBlurPx *
                        underlayBlurProgress.coerceIn(0f, 1f),
                ) {
                    if (title != null && !wide) {
                        if (!com.aritxonly.teak.common.theme.TeakUiStyle.isClassic &&
                            largeTitleState.present > 0
                        ) {
                            Layout(
                                content = {
                                    TeakLargeTitleForeground(
                                        state = largeTitleState,
                                        pageState = pageState,
                                        topInset = largeTitleTopInset,
                                        underlayLiftPx = underlayLiftPx,
                                        underlayAlpha = underlayAlpha,
                                        underlayBlurProgress = underlayBlurProgress,
                                    )
                                    TeakTopBar(
                                        title = title,
                                        collapse = pageState.collapse,
                                        titleAlwaysVisible = titleAlwaysVisible,
                                        modifier = Modifier.searchChromeMotion(incoming = false),
                                    )
                                },
                            ) { measurables, constraints ->
                                val titlePlaceable = measurables[0].measure(
                                    constraints.copy(minHeight = 0),
                                )
                                val barPlaceable = measurables[1].measure(
                                    constraints.copy(minHeight = 0),
                                )
                                // 只上报顶栏尺寸；大标题允许画到布局边界之外（不裁剪）。
                                layout(barPlaceable.width, barPlaceable.height) {
                                    titlePlaceable.placeRelative(0, 0)
                                    barPlaceable.placeRelative(0, 0)
                                }
                            }
                        } else {
                            TeakTopBar(
                                title = title,
                                collapse = pageState.collapse,
                                titleAlwaysVisible = titleAlwaysVisible,
                                modifier = Modifier.searchChromeMotion(incoming = false),
                            )
                        }
                        // 搜索层自己的顶栏：旧顶栏缩进淡出的同时，这一份跟着搜索结果从下往上抬进来。
                        // 只画小标题，材质仍由上一步的同一个 ProgressiveTopBarMaterial 提供。
                        // 折叠进度要等搜索层真正接管滚动后再读：进场那一帧 pageState 还是旧列表的，
                        // 直接读会让搜索标题在没有滚动时闪一下。
                        if (searchTitle != null) {
                            val settled = searchTitleProgress >= 0.999f
                            TeakTopBar(
                                title = searchTitle,
                                collapse = if (settled) pageState.collapse else 0f,
                                barBlurFraction = 0f,
                                titleAlwaysVisible = titleAlwaysVisible && settled,
                                modifier = Modifier.searchChromeMotion(incoming = true),
                            )
                        }
                    }
                    if (wide && title != null) {
                        WideFoldTopAppBar(
                            title = title,
                            overlayTitle = searchTitle,
                            overlayTitleProgress = searchTitleProgress,
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
                            revealed = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
                                true
                            } else {
                                backRevealed ?: pageState.backRevealed
                            },
                            onBack = if (showBackButton) onBack else null,
                            light = backRevealed == false,
                            glyphColor = topBarGlyphColor,
                            leading = topBarLeading,
                            center = topBarCenter,
                            trailing = topBarActions,
                            modifier = Modifier.searchChromeMotion(incoming = false),
                        )
                        // 搜索层自己的操作行：只有返回键，和旧顶栏交叉淡入淡出。
                        if (searchTitle != null) {
                            TeakTopOverlay(
                                revealed = backRevealed ?: pageState.backRevealed,
                                onBack = if (showBackButton) onBack else null,
                                light = backRevealed == false,
                                glyphColor = topBarGlyphColor,
                                modifier = Modifier.searchChromeMotion(incoming = true),
                            )
                        }
                    }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                menuOverlay()
            }
            Box(modifier = Modifier.fillMaxSize()) {
                topOverlay()
            }
        }
    }
}
