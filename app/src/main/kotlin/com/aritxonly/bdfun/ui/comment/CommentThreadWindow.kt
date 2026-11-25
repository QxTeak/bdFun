package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

@Composable
internal fun ConfigureCommentThreadWindow(
    playerBottomDp: androidx.compose.ui.unit.Dp?, minTopPx: Float, paneWidth: androidx.compose.ui.unit.Dp?,
) {
    val VideoPageBus = LocalVideoPageState.current
        val view = androidx.compose.ui.platform.LocalView.current
        val dialogWindow = (view.parent as? androidx.compose.ui.window.DialogWindowProvider)?.window
        val density = LocalDensity.current
        // 播放器块当前的下沿（px）→ 这一页的顶边。播放器被顶上去多少，这里就跟着上移多少。
        // 动态详情没有播放器，用 minTopPx 把面板顶边压在返回键下面，不让它顶到屏幕最上面。
        val panelTopPx = maxOf(
            // 和播放器底边重叠 2dp，抵消窗口位置更新比播放器慢一帧的问题，
            // 避免往上顶时在接缝处露出主评论区。
            (playerBottomDp?.let { with(density) { it.toPx() } }
                ?: VideoPageBus.playerBottomPx).coerceAtLeast(0f) -
                with(density) { 2.dp.toPx() },
            minTopPx,
        )
        val screenHeightPx = with(density) {
            androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp.toPx()
        }
        androidx.compose.runtime.SideEffect {
            dialogWindow?.setDimAmount(0f)
            // 只保留内容自己的「非线性上移」，关掉弹窗窗口自带的缩放/淡入动画。
            dialogWindow?.setWindowAnimations(0)
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                dialogWindow?.isNavigationBarContrastEnforced = false
            }
            @Suppress("DEPRECATION")
            dialogWindow?.navigationBarColor = android.graphics.Color.TRANSPARENT
            // 关键：弹窗窗口只从播放器下沿开始。窗口不覆盖播放器，点视频才能正常呼出控件；
            // 同时只留返回键能关闭这一页，点别处不再触发 onDismiss。
            dialogWindow?.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            )
        }
        // 窗口位置在组合提交阶段直接设置，和播放器同一帧更新；
        // 只在数值真的变化时才重写窗口参数，避免列表滚动时每帧都改窗口。
        val panelTopInt = panelTopPx.toInt()
        val paneWidthPx = paneWidth?.let { with(density) { it.roundToPx() } }
            ?: android.view.ViewGroup.LayoutParams.MATCH_PARENT
        val paneEndPx = if (paneWidth != null) WindowInsets.safeDrawing.getRight(
            density, androidx.compose.ui.unit.LayoutDirection.Ltr,
        ) else 0
        val panelHeightPx = (screenHeightPx - panelTopPx).toInt().coerceAtLeast(1)
        val lastPanelGeometry = remember { intArrayOf(Int.MIN_VALUE, 0, 0, 0) }
        androidx.compose.runtime.SideEffect {
            val geometry = intArrayOf(panelTopInt, paneWidthPx, panelHeightPx, paneEndPx)
            if (lastPanelGeometry.contentEquals(geometry)) return@SideEffect
            geometry.copyInto(lastPanelGeometry)
            val window = dialogWindow ?: return@SideEffect
            val lp = window.attributes ?: return@SideEffect
            lp.gravity = android.view.Gravity.TOP or android.view.Gravity.RIGHT
            lp.width = paneWidthPx
            lp.height = panelHeightPx
            lp.x = paneEndPx
            lp.y = panelTopInt
            window.attributes = lp
        }

}
