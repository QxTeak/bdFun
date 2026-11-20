package com.aritxonly.bdfun.core.domain

import kotlin.math.abs
import kotlin.math.roundToInt

/** 旋转只为紧凑设备自动进入全屏；展开的大屏保留分栏和手动全屏选择。 */
fun fullscreenAfterVideoRotation(landscape: Boolean, smallestWidthDp: Int, autoRotate: Boolean): Boolean? = when {
    !landscape -> false
    autoRotate && smallestWidthDp in 1 until 600 -> true
    else -> null
}

/** 视频宽高比：接口未给出有效宽高时为空，交给调用方按横屏兜底。 */
fun videoAspectRatio(width: Int, height: Int): Float? =
    if (width > 0 && height > 0) width.toFloat() / height else null

/** 竖屏视频：宽高比小于 1；未知比例按横屏处理，避免误判导致全屏方向错误。 */
fun isVerticalVideoAspect(aspect: Float?): Boolean =
    aspect != null && aspect > 0f && aspect < 1f

/**
 * 画中画比例：把浮点宽高比近似成 1..100 的小整数比（系统要求分子分母都在此范围），
 * 竖屏视频自然得到 9:16，横屏得到 16:9；无法换算时回退 16:9。
 */
fun pipAspectPair(aspect: Float?): Pair<Int, Int> {
    if (aspect == null || aspect.isNaN() || aspect <= 0f) return 16 to 9
    var bestNum = 16
    var bestDen = 9
    var bestError = Float.MAX_VALUE
    for (den in 1..100) {
        val num = (aspect * den).roundToInt()
        if (num !in 1..100) continue
        val error = abs(aspect - num.toFloat() / den)
        if (error < bestError) {
            bestError = error
            bestNum = num
            bestDen = den
        }
    }
    return bestNum to bestDen
}
