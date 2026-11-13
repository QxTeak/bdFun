package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.core.domain.targetPlaybackRefreshRate
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.SystemClock
import android.view.Window
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference

/**
 * 智能刷新率：
 * - 无视频播放且静止：锁到 1 帧（省电），手指一碰立刻恢复系统满帧；
 * - 有视频播放且静止：锁 60 帧；
 * - 全屏播放且静止：同样 60 帧。
 * 通过 View.setRequestedFrameRate 生效（API 35+），同时清掉 preferredRefreshRate 让系统自己选模式。
 */
private class WindowRefreshStateHolder {

    private var lastTouchAt = SystemClock.uptimeMillis()
    private var applied = Float.NaN
    private var windowRef: WeakReference<Window>? = null

    fun attach(window: Window?) {
        windowRef = window?.let { WeakReference(it) }
        applied = Float.NaN
        runCatching {
            if (Build.VERSION.SDK_INT >= 35) {
                // 手指一碰系统会自己把帧率拉满，避免低帧下第一下滑动卡顿
                window?.setFrameRateBoostOnTouchEnabled(true)
            }
        }
    }

    fun detach() {
        runCatching {
            if (Build.VERSION.SDK_INT >= 35) {
                windowRef?.get()?.decorView?.setRequestedFrameRate(0f)
            }
        }
        windowRef = null
        applied = Float.NaN
    }

    fun touch() {
        lastTouchAt = SystemClock.uptimeMillis()
    }

    fun apply(enabled: Boolean, playing: Boolean, fullscreen: Boolean) {
        if (Build.VERSION.SDK_INT < 35) return
        val window = windowRef?.get() ?: return
        val target = targetPlaybackRefreshRate(
            enabled, SystemClock.uptimeMillis() - lastTouchAt, playing, fullscreen,
        )
        if (target == applied) return
        applied = target
        runCatching {
            // API 35+：给窗口一个期望帧率（0 = 取消限制，恢复系统默认）
            window.decorView.setRequestedFrameRate(target)
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** 挂在页面根节点：收集触摸 + 定时应用刷新率。 */
@Composable
fun Modifier.smartRefresh(enabled: Boolean): Modifier {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val controller = remember(activity) { WindowRefreshStateHolder() }
    val playing = BackgroundPlayback.playing
    val fullscreen = BackgroundPlayback.fullscreen
    DisposableEffect(activity) {
        controller.attach(activity?.window)
        onDispose { controller.detach() }
    }
    LaunchedEffect(enabled, playing, fullscreen) {
        while (true) {
            controller.apply(enabled, playing, fullscreen)
            delay(400)
        }
    }
    return this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                if (event.changes.any { it.pressed }) controller.touch()
            }
        }
    }
}
