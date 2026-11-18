package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/** 顶栏与列表共用窗口判断，旋转时同时切换行数与顶部留白。 */
@Composable
internal fun useSingleRowSearchResults(): Boolean {
    val window = LocalWindowInfo.current.containerSize
    return window.width > window.height && with(LocalDensity.current) { window.width.toDp() >= 600.dp }
}
