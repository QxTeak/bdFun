package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache

/**
 * 列表缩略图的内存缓存，给查看器做「起飞/回落」那一帧用。
 *
 * Telegram 的 PhotoViewer 直接用 ImageReceiver 里已有的缩略图 bitmap 跑动画，
 * 不在动画期间解码；这里在列表 AsyncImage 成功时顺手记一份，查看器打开时就能
 * 立刻拿到 bitmap，而不是再等 Coil 请求一次。
 */
internal object PhotoThumbCache {

    // sizeOf 返回 KB，所以上限也是 KB；32 会让任何一张缩略图都立即被淘汰。
    private val cache = object : LruCache<String, Bitmap>(64 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    fun put(url: String, bitmap: Bitmap?) {
        if (url.isBlank() || bitmap == null || bitmap.isRecycled) return
        cache.put(url, bitmap)
    }

    fun get(url: String): Bitmap? = cache.get(url)?.takeIf { !it.isRecycled }
}

/** Coil 的 drawable 可能是 BitmapDrawable，也可能是其它 Drawable；统一取一张可绘制的 bitmap。 */
internal fun Drawable.toPhotoBitmapOrNull(): Bitmap? {
    if (this is BitmapDrawable) {
        val bitmap = bitmap ?: return null
        return if (bitmap.isRecycled) null else bitmap
    }
    val width = intrinsicWidth
    val height = intrinsicHeight
    if (width <= 0 || height <= 0) return null
    return try {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            draw(Canvas(it))
        }
    } catch (_: Throwable) {
        null
    }
}
