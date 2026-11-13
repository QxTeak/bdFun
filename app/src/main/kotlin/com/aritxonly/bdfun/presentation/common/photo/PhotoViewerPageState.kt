package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * 单页的手势与图片状态：宽高比、缩放倍率、放大后的平移。
 *
 * 按页持有，翻页时互不影响。放在 StateHolder 而不是 composable 局部变量里，
 * 是项目约定（mutableStateOf 只允许出现在 StateHolder 内部）。
 */
internal class PhotoViewerPageState {
    var aspect by mutableFloatStateOf(0f)
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
}
