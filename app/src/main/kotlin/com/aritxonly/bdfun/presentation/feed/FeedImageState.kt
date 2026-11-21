package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Rect

/**
 * 列表滚动时 `onGloballyPositioned` 每帧都会写；菜单/查看器只在点击时读一次，
 * 用普通 Map 即可，避免 mutableStateMap 的逐帧快照通知。
 */
internal class FeedImageBoundsStateHolder { val bounds = mutableMapOf<String, Rect>() }
