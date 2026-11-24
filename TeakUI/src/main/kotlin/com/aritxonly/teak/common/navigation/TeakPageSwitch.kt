package com.aritxonly.teak.common.navigation

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween

/**
 * 小米官方横向页面切换参数。
 *
 * 取自工作区里 MIUI 文件管理 9.3.0.8 的反编译：
 * `com.android.fileexplorer.view.ScrollControlViewPagerV2` 给 ViewPager 换掉了默认
 * scroller，用的是 `ViewPagerScroller(context, DecelerateInterpolator(1.5f))`，
 * 并且把 `startScroll` 的时长固定成 **350ms**（`pad/fragment/ViewPagerScroller.java`）。
 *
 * `DecelerateInterpolator(1.5f)` 的曲线是 `1 - (1 - t)^3`，也就是三次方减速：
 * 起步快、尾巴慢。点 tab 和手滑松手吸附都走同一条曲线，手感才一致。
 */
const val TeakPageSwitchDurationMs = 350

val TeakPageSwitchEasing: Easing = Easing { t ->
    val left = 1f - t
    1f - left * left * left
}

fun TeakPageSwitchSpec(): AnimationSpec<Float> = PageSwitchSpec

/**
 * 共享同一个 spec 实例：pager 在滚动时每帧都会重组，
 * 每次 `tween()` 新对象会让 `PagerDefaults.flingBehavior` 的 remember 失效。
 */
private val PageSwitchSpec: AnimationSpec<Float> =
    tween(durationMillis = TeakPageSwitchDurationMs, easing = TeakPageSwitchEasing)
