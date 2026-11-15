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
    /**
     * 初始值用缩略图的 16:9 —— 列表里的图都是 `@<w>w_<h>h_1c` 的 16:9 裁切，
     * 这样入场第一帧的几何和卡片上的缩略图完全一致；原图加载好后再换成真实宽高比。
     */
    var aspect by mutableFloatStateOf(16f / 9f)
    var zoom by mutableFloatStateOf(1f)
    var pan by mutableStateOf(Offset.Zero)
}
