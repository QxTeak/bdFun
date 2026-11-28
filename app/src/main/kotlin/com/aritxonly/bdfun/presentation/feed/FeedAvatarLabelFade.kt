package com.aritxonly.bdfun.ui

import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * 顶栏往下划时，头像下面的名称随渐进模糊一起淡出（UP 页顶栏用）。
 *
 * 用 CompositionLocal 而不是参数：头像项由 LazyRow 的 item builder 组装，
 * 逐层加参数会把已经 12 个参数的头像项再撑大一圈。
 * null = 名称常显（动态页那张头像卡片走这条，那里没有滚动驱动的材质）。
 */
internal val LocalFeedAvatarLabelFade = staticCompositionLocalOf<State<Float>?> { null }

/**
 * 名称淡出只改绘制期 alpha：**不动布局**，所以头像行高度、列表顶部留白、卡片位置都不变。
 * [fade] 的 0 → 1 与材质的出现共用同一条补间，因此是同一拍。
 */
internal fun Modifier.feedAvatarLabelFade(fade: State<Float>?): Modifier =
    if (fade == null) this else graphicsLayer { alpha = 1f - fade.value.coerceIn(0f, 1f) }
