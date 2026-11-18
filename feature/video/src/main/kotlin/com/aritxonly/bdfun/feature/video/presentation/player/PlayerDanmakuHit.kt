package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/** 以窗口坐标连接手势层和变换后的弹幕层，不向 feature 暴露渲染引擎。 */
class PlayerDanmakuHit {
    val likes = kotlinx.coroutines.flow.MutableStateFlow<Set<Long>>(emptySet())
    var like: ((Long) -> Unit)? = null
    var coordinates: LayoutCoordinates? = null
    var hotCapture: ((Offset) -> PlayerDanmakuPress?)? = null
    var capture: ((Offset) -> PlayerDanmakuPress?)? = null
    fun at(position: Offset): PlayerDanmakuPress? {
        val layout = coordinates?.takeIf { it.isAttached } ?: return null
        val window = layout.localToWindow(position)
        return hotCapture?.invoke(window) ?: capture?.invoke(window)
    }
}

class PlayerDanmakuPress(val confirm: () -> Unit, val cancel: () -> Unit)
