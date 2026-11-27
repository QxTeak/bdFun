package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest

/**
 * 列表项会在滚动/状态变化时反复重组。把 ImageRequest 记住，避免每次都新建请求对象，
 * 也让 Coil 能稳定复用同一份缓存 key。
 */
@Composable
internal fun rememberImageRequest(
    data: Any?,
    /** 列表里默认不做淡入：滚动时大量 crossfade 会同时跑动画，是列表掉帧的常见来源。 */
    crossfade: Boolean = false,
    /** 给列表缩略图一个解码上限，避免原图尺寸直接进内存/纹理。 */
    size: Int? = null,
): ImageRequest {
    val context = LocalContext.current
    return remember(data, crossfade, size) {
        ImageRequest.Builder(context)
            .data(data)
            .apply {
                if (crossfade) crossfade(true)
                if (size != null) size(size)
            }
            .build()
    }
}
