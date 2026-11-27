package com.aritxonly.bdfun.core.domain

/** 旋转只为紧凑设备自动进入全屏；展开的大屏保留分栏和手动全屏选择。 */
fun fullscreenAfterVideoRotation(landscape: Boolean, smallestWidthDp: Int, autoRotate: Boolean): Boolean? = when {
    !landscape -> false
    autoRotate && smallestWidthDp in 1 until 600 -> true
    else -> null
}

/** 传感器姿态稳定后要请求的朝向；null 表示不干预。 */
enum class VideoRotationTarget { Landscape, Portrait }

/**
 * 视频页姿态监听要请求的朝向。
 *
 * - 关闭自动旋转 / 已锁定方向：不干预；
 * - 已经全屏：姿态翻转就跟着转（横屏 ↔ 竖屏）；
 * - 还没全屏：只有紧凑手机转到横屏才请求横屏，交给 [fullscreenAfterVideoRotation]
 *   把页面带进横屏全屏。平板横屏本来就是分栏，不能在这里抢系统旋转。
 * - 半屏时的竖屏姿态不做事：页面本来就是锁竖屏的，再请求一次只会和 idle 锁打架。
 */
fun rotationTargetForPose(
    landscape: Boolean,
    fullscreen: Boolean,
    smallestWidthDp: Int,
    autoRotate: Boolean,
    orientationLocked: Boolean,
): VideoRotationTarget? {
    if (!autoRotate || orientationLocked) return null
    if (!fullscreen && !(landscape && smallestWidthDp in 1 until 600)) return null
    return if (landscape) VideoRotationTarget.Landscape else VideoRotationTarget.Portrait
}
