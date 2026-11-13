package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect

/**
 * 一次「打开大图」的请求。
 *
 * origin 是缩略图在根布局里的矩形，用来做共享元素入场：p=0 时图片停在这个矩形上，
 * p=1 时铺满全屏。没有 origin（比如从通知里直接打开）时退化成从中心放大。
 */
@Immutable
internal data class PhotoViewerRequest(
    val images: List<String>,
    val index: Int,
    val origin: Rect? = null,
    val originRadiusPx: Float = 0f,
    /** 每张图自己的缩略图矩形（与 images 同序）；缺省的那张没有共享元素起点。 */
    val origins: List<Rect?> = emptyList(),
    /** 底部的操作区由发起方提供，宿主不需要知道这里是动态还是评论。 */
    val footer: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
) {
    /** 当前页的收拢目标：优先用它自己的缩略图，没有才退回点开那张的起点。 */
    fun originFor(page: Int): Rect? =
        origins.getOrNull(page) ?: if (page == index) origin else null
}

/**
 * 图片查看器的页面级状态。
 *
 * 只在 request 变化时驱动动画；入场/退场的进度由 [PhotoViewerRoot] 自己持有，
 * 因为它是纯展示状态，页面不需要知道。
 */
internal class PhotoViewerStateHolder {
    var request by mutableStateOf<PhotoViewerRequest?>(null)
        private set

    fun open(
        images: List<String>,
        index: Int,
        origin: Rect? = null,
        originRadiusPx: Float = 0f,
        origins: List<Rect?> = emptyList(),
        footer: (@Composable (dismiss: () -> Unit) -> Unit)? = null,
    ) {
        if (images.isEmpty()) return
        request = PhotoViewerRequest(
            images = images,
            index = index.coerceIn(0, images.lastIndex),
            origin = origin,
            originRadiusPx = originRadiusPx,
            origins = origins,
            footer = footer,
        )
    }

    fun close() {
        request = null
    }
}

internal val LocalPhotoViewer = staticCompositionLocalOf<PhotoViewerStateHolder> {
    error("图片查看器必须放在 PhotoViewerRoot 里面")
}
