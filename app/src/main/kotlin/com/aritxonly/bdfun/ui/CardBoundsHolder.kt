package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow

/**
 * 卡片在窗口里的矩形。
 *
 * 刻意不用 Compose State：卡片滚动时 `onGloballyPositioned` 每帧都会回调，
 * 写 `mutableStateOf` 会让每张可见卡片逐帧重组。
 *
 * 现在连换算也推迟到真正需要时：布局回调只保存坐标引用（不分配 Rect、不做矩阵计算），
 * 长按/省略号打开菜单时才调用 [value]。少数需要屏幕坐标的卡片仍可直接写 [value]。
 */
internal class CardBoundsHolder {
    private var coordinates: LayoutCoordinates? = null
    private var explicit: Rect = Rect.Zero

    var value: Rect
        get() = coordinates?.takeIf { it.isAttached }?.boundsInWindow() ?: explicit
        set(rect) {
            coordinates = null
            explicit = rect
        }

    /** 只记录本次布局坐标；滚动期间不做任何分配。 */
    fun attach(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }
}

/** 长按起点（卡片内坐标）。只在指针事件与长按回调里读写，同样不需要 Compose State。 */
internal class CardPressPoint {
    var value: Offset = Offset.Zero
}
