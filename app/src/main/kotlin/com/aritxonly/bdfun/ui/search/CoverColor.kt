package com.aritxonly.bdfun.ui

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color

/**
 * 取封面图的主色，给「删除粒子爆炸」当碎屑颜色。
 *
 * 为什么不用主题强调色：粒子的语义是「这一项自己碎掉了」，视频卡片的身份色就是它的封面。
 * 只取 3×3 个点做平均，代价可以忽略。
 *
 * 两个必须处理的边界：
 * - Coil 默认解出 `Config#HARDWARE` 位图，`getPixel` 会直接抛异常，必须先转成软件位图；
 * - 封面可能是动图/已被回收，任何取值失败都只当「没有颜色」，不允许把列表带崩。
 */
internal fun coverAverageColor(bitmap: Bitmap): Color? = runCatching {
    if (bitmap.isRecycled || bitmap.width <= 0 || bitmap.height <= 0) return@runCatching null
    val sampled = if (bitmap.config == Bitmap.Config.HARDWARE) {
        bitmap.copy(Bitmap.Config.ARGB_8888, false)
    } else {
        null
    }
    val source = sampled ?: bitmap
    try {
        averageOfSamples(source)
    } finally {
        // 只回收我们自己的临时副本；Drawable 持有的那张不能动。
        sampled?.recycle()
    }
}.getOrNull()

private fun averageOfSamples(bitmap: Bitmap): Color? {
    val width = bitmap.width
    val height = bitmap.height
    var red = 0f
    var green = 0f
    var blue = 0f
    var samples = 0f
    for (fx in SampleFractions) {
        for (fy in SampleFractions) {
            val x = (width * fx).toInt().coerceIn(0, width - 1)
            val y = (height * fy).toInt().coerceIn(0, height - 1)
            val pixel = bitmap.getPixel(x, y)
            if (android.graphics.Color.alpha(pixel) < 0x33) continue
            red += android.graphics.Color.red(pixel)
            green += android.graphics.Color.green(pixel)
            blue += android.graphics.Color.blue(pixel)
            samples += 1f
        }
    }
    if (samples <= 0f) return null
    return Color(red = red / samples / 255f, green = green / samples / 255f, blue = blue / samples / 255f)
}

/** 采样点避开最外圈：封面上下常有黑边/渐变蒙层，直接取角会偏暗。 */
private val SampleFractions = listOf(0.22f, 0.5f, 0.78f)
