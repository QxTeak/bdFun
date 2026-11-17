package com.aritxonly.bdfun.ui

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.view.RoundedCorner
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp

/**
 * 设备屏幕的物理圆角半径（px）。
 *
 * 优先用 Android 12+ 的 [RoundedCorner]（四角取最大值，个别机型上下角不同）；
 * 读不到时退回 framework 的 `rounded_corner_radius*` dimen；再没有就返回 0，
 * 由调用方保留原有固定值，不会把界面画成直角以外的怪形状。
 */
fun displayCornerRadiusPx(context: Context): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
    val display = context.display
    val reported = ROUNDED_CORNER_POSITIONS.maxOfOrNull { position ->
        runCatching { display.getRoundedCorner(position)?.radius ?: 0 }.getOrDefault(0)
    } ?: 0
    return reported.takeIf { it > 0 } ?: frameworkCornerRadiusPx(context)
}

/** 窗口级读取：分屏/小窗里拿到的是这个窗口自己的圆角，桌面全屏时与 [displayCornerRadiusPx] 一致。 */
fun viewCornerRadiusPx(view: View): Int {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return 0
    val insets = view.rootWindowInsets ?: return 0
    val reported = ROUNDED_CORNER_POSITIONS.maxOfOrNull { position ->
        runCatching { insets.getRoundedCorner(position)?.radius ?: 0 }.getOrDefault(0)
    } ?: 0
    return reported.takeIf { it > 0 } ?: frameworkCornerRadiusPx(view.context)
}

/** 只在 framework 暴露 `rounded_corner_radius_bottom` 的机器上有效，其余情况返回 0。 */
@SuppressLint("DiscouragedApi")
private fun frameworkCornerRadiusPx(context: Context): Int {
    val id = context.resources.getIdentifier("rounded_corner_radius_bottom", "dimen", "android")
    return if (id > 0) runCatching { context.resources.getDimensionPixelSize(id) }.getOrDefault(0) else 0
}

@Composable
fun rememberDisplayCornerRadiusDp(): androidx.compose.ui.unit.Dp {
    val context = LocalContext.current
    val view = LocalView.current
    val density = androidx.compose.ui.platform.LocalDensity.current.density
    val radiusPx = remember(context, view) {
        viewCornerRadiusPx(view).takeIf { it > 0 } ?: displayCornerRadiusPx(context)
    }
    return if (radiusPx > 0) (radiusPx / density).dp else 0.dp
}

private val ROUNDED_CORNER_POSITIONS = intArrayOf(
    RoundedCorner.POSITION_TOP_LEFT,
    RoundedCorner.POSITION_TOP_RIGHT,
    RoundedCorner.POSITION_BOTTOM_LEFT,
    RoundedCorner.POSITION_BOTTOM_RIGHT,
)
