package com.aritxonly.bdfun.infra.playback

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader

/** 页面与服务必须提交相同形状，避免系统切换媒体会话时封面圆方跳变。 */
internal fun mediaArtwork(source: Bitmap): Bitmap {
    val side = minOf(source.width, source.height).coerceAtLeast(1)
    val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    // 沿用后台服务既有裁切坐标，避免本次封面修复改变取景。
    paint.shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
    val radius = side / 2f
    Canvas(output).drawCircle(radius, radius, radius, paint)
    return output
}
