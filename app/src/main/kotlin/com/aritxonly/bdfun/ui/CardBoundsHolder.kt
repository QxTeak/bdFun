package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Rect

/**
 * 卡片在窗口里的矩形。
 *
 * 刻意不用 Compose State：卡片滚动时 `onGloballyPositioned` 每帧都会回调，
 * 写 `mutableStateOf` 会让每张可见卡片逐帧重组。菜单/长按只在事件发生时读一次，
 * 用普通可变字段即可。
 */
internal class CardBoundsHolder {
    var value: Rect = Rect.Zero
}
