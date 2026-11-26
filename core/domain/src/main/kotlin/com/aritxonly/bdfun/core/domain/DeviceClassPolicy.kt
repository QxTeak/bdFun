package com.aritxonly.bdfun.core.domain

/**
 * 设备尺寸分类。
 *
 * 用旋转前后都不变的 `smallestScreenWidthDp` 判断，等价于 Android 的 `sw600dp`
 * 资源限定符，也与 Material 3 WindowSizeClass 的 compact / medium 边界一致。
 * 不能拿旋转后的 `screenWidthDp` 判断：直板机横屏也会超过 600dp，
 * 那会把手机误判成平板并进入宽屏分栏。
 */
const val TabletSmallestWidthDp = 600

fun isTabletDevice(smallestWidthDp: Int): Boolean =
    smallestWidthDp >= TabletSmallestWidthDp
