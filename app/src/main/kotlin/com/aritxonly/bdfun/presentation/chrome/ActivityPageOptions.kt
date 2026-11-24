package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.RowScope

@Immutable
internal data class ActivityPageOptions(val backRevealed: Boolean? = null, val showBackButton: Boolean = true,
    val title: String? = null, val limitContentWidth: Boolean = true, val showTopBar: Boolean = true,
    val showScrollTop: Boolean = true, val titleAlwaysVisible: Boolean = false,
    val topBarMaterialProgress: Float? = null, val barBlurFraction: Float = 1f, val topBarGlyphColor: Color? = null,
    /** 整页搜索「一镜到底」：底层内容退让进度与顶栏透明度，默认不参与。 */
    val underlay: PageUnderlayOptions = PageUnderlayOptions())

/** 页面一镜到底的一组动画参数，集中成一个对象避免 options 构造函数过长。 */
@Immutable
internal data class PageUnderlayOptions(
    val progress: Float = 0f,
    val alpha: Float = 1f,
    val blur: Float = 0f,
    val topBarAlpha: Float = 1f,
)
@Immutable
internal data class ActivityDiscOptions(val showBackgroundDisc: Boolean = true,
    val discLift: Dp = 0.dp, val discEndPadding: Dp? = null)

/** 底部有搜索框或回复条时，唱片的统一抬升高度（避开 64dp 控件 + 间距）。 */
internal val BackgroundDiscDockLift = 74.dp
@Immutable
internal data class ActivityPageSlots(
    val topBarLeading: @Composable RowScope.() -> Unit = {},
    val topBarCenter: @Composable BoxScope.() -> Unit = {},
    val topBarActions: @Composable RowScope.() -> Unit = {},
    val overlay: @Composable BoxScope.() -> Unit = {},
    /** 最顶层覆盖：需要盖住页面顶栏的全屏页（如媒体库搜索）。 */
    val topOverlay: @Composable BoxScope.() -> Unit = {},
    val menuOverlay: @Composable BoxScope.() -> Unit = {},
)
