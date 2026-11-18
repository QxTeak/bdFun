package com.aritxonly.teak.common.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** 从任意 Context 往上找宿主 Activity；找不到就返回 null。 */
fun Context.hostActivity(): Activity? {
    var ctx: Context? = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

/** 让 Dialog 窗口的状态栏 / 导航栏图标明暗和宿主页面完全一致。 */
fun matchSystemBars(dialogWindow: Window?, hostWindow: Window?) {
    if (dialogWindow == null || hostWindow == null) return
    runCatching {
        val host = WindowCompat.getInsetsController(hostWindow, hostWindow.decorView)
        val dialog = WindowCompat.getInsetsController(dialogWindow, dialogWindow.decorView)
        dialog.isAppearanceLightStatusBars = host.isAppearanceLightStatusBars
        dialog.isAppearanceLightNavigationBars = host.isAppearanceLightNavigationBars
        // 视频页等隐藏状态栏的场景，菜单窗口也不要把它重新显示出来。
        val hostInsets = ViewCompat.getRootWindowInsets(hostWindow.decorView)
        if (hostInsets != null &&
            !hostInsets.isVisible(WindowInsetsCompat.Type.statusBars())
        ) {
            dialog.hide(WindowInsetsCompat.Type.statusBars())
        }
    }
}
