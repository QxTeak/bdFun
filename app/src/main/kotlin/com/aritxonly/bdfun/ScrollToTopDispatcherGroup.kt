package com.aritxonly.bdfun

import android.content.Context
import android.view.ViewGroup
import com.aritxonly.teak.common.chrome.TeakStatusBarTapBridge

/**
 * 承接 HyperOS / MIUI 的系统级“点状态栏回顶”。
 *
 * 状态栏触摸归 SystemUI，应用拿不到 TouchListener；但 MIUI 的 ViewRootImpl 在窗口获得
 * 焦点后会注册 `IScrollToTopListener`，收到状态栏点击时在窗口里找“可滚动 View”并调用
 * 它的 scroll 方法。判定要求目标必须是 **ViewGroup**，且 `computeVerticalScrollRange > 0`、
 * `computeVerticalScrollOffset > 0`、`canScrollVertically(-1)` 为真、可见面积不小于屏幕 1/3。
 *
 * Compose 的根 View 不满足这套判定，所以这里垫一个透明的全屏 ViewGroup，谎报这几个
 * 滚动指标；MIUI 调它的 `scrollBy` 时，转给当前页面的回顶动作
 * （[TeakStatusBarTapBridge]，由 `TeakStatusBarScrollToTop` 登记）。
 *
 * 无子 View、不绘制、不接收点击，放在内容层最底下；对布局和绘制没有实际开销。
 */
internal class ScrollToTopDispatcherGroup(context: Context) : ViewGroup(context) {
    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) = Unit

    override fun computeVerticalScrollOffset(): Int = 1

    override fun computeVerticalScrollExtent(): Int = 1

    override fun computeVerticalScrollRange(): Int = 2

    override fun canScrollVertically(direction: Int): Boolean = true

    override fun scrollTo(x: Int, y: Int) {
        dispatch()
    }

    override fun scrollBy(x: Int, y: Int) {
        dispatch()
    }

    private fun dispatch() {
        TeakStatusBarTapBridge.request()
    }
}
