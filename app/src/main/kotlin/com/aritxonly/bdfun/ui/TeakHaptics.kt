package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

/**
 * 统一的轻触反馈。
 *
 * 用系统默认的按键/上下文触感，不再自己写 Vibrator 时长和振幅：
 * 不同设备的振动器强度差异很大，写死的参数在有的机器上会过重。
 */
internal class TeakHaptics(private val view: View) {
    fun tap() {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    fun contextClick() {
        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
    }

    fun longPress() {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }
}

@Composable
internal fun rememberTeakHaptics(): TeakHaptics {
    val view = LocalView.current
    return remember(view) { TeakHaptics(view) }
}

/** 非 Composable 的路由入口（打开视频等）用：沿 ContextWrapper 找 Activity 的 decorView。 */
internal fun Context.teakTapHaptic() {
    var current: Context? = this
    while (current != null) {
        if (current is Activity) {
            current.window.decorView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        current = (current as? ContextWrapper)?.baseContext
    }
}
