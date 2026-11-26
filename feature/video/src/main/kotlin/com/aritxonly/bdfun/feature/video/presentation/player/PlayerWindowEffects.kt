package com.aritxonly.bdfun.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aritxonly.bdfun.core.domain.isTabletDevice
import com.aritxonly.bdfun.core.model.video.Quality

/**
 * 全屏期间由我们这一侧写进窗口的朝向：
 * `BindPlayerOrientation` 写的 LOCKED / SENSOR_LANDSCAPE / SENSOR，
 * 以及 `VideoActivity` 姿态监听写的 LANDSCAPE / PORTRAIT。
 *
 * `VideoActivity` 只在 `!fullscreen` 时把窗口交还系统，而横屏退回竖屏那一刻
 * `onConfigurationChanged` 跑在 Compose 把 fullscreen 改回 false 之前，
 * 那时它看到的还是全屏，补不上这一手；退出全屏时若朝向仍是这些值，就得自己还原。
 */
private val playerForcedOrientations = setOf(
    ActivityInfo.SCREEN_ORIENTATION_LOCKED,
    ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE,
    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
    ActivityInfo.SCREEN_ORIENTATION_SENSOR,
)

@Composable
fun BindPlayerSystemBars(
    fullscreen: Boolean,
    immersiveStatusBar: Boolean,
    ui: VideoPlayerUiStateHolder,
    portraitVideo: Boolean = false,
) {
    val activity = LocalContext.current as? Activity
    val tablet = isTabletDevice(LocalConfiguration.current.smallestScreenWidthDp)
    var inlineOrientation by ui.inlineOrientation
    DisposableEffect(fullscreen, immersiveStatusBar, portraitVideo, tablet) {
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        if (fullscreen) {
            inlineOrientation = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            controller?.apply {
                systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
            // 竖屏视频：保持竖屏全屏，铺满整屏；横屏视频仍走原来的横屏全屏。
            activity?.requestedOrientation = if (portraitVideo) {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR
            } else {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        } else {
            if (immersiveStatusBar) {
                // 沉浸模式：半屏也不显示状态栏时间等信息，把状态栏一起藏掉；
                // 导航栏保留，手势条不受影响。
                controller?.apply {
                    systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    show(WindowInsetsCompat.Type.navigationBars())
                    hide(WindowInsetsCompat.Type.statusBars())
                }
            } else {
                controller?.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            // 退出全屏后详情页要是被钉在某个方向，就只能退出视频重进才恢复旋转，
            // 所以在放开 systemBars 之后，只把仍被我们钉住的朝向还给系统。
            if (fullscreen) {
                val current = activity?.requestedOrientation
                activity?.requestedOrientation = if (current in playerForcedOrientations) {
                    if (tablet) ActivityInfo.SCREEN_ORIENTATION_USER
                    else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    inlineOrientation
                }
            }
        }
    }

}

@Composable
fun BindPlayerHdrWindow(quality: Quality?, fullscreen: Boolean) {
    val activity = LocalContext.current as? Activity
    // HDR / 杜比视界：把屏幕亮度拉到最高，请求 HDR 颜色模式与 10bit 输出。
    // 部分机型不会因为 Surface 是 HDR 就自动提亮，这里显式做一次。
    val isHdrQuality = quality?.let { q ->
        q.id == 125 || q.id == 126 ||
            q.codec.contains("hdr", ignoreCase = true) ||
            q.codec.contains("dovi", ignoreCase = true) ||
            q.codec.contains("dvh", ignoreCase = true) ||
            q.codec.contains("dolby", ignoreCase = true)
    } == true
    DisposableEffect(isHdrQuality, fullscreen, activity) {
        val window = activity?.window
        val hdrActive = isHdrQuality
        val previousBrightness = window?.attributes?.screenBrightness ?: -1f
        val previousColorMode = window?.attributes?.colorMode ?: 0
        if (hdrActive && window != null) {
            runCatching {
                window.attributes = window.attributes.apply {
                    screenBrightness =
                        android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                    colorMode = android.content.pm.ActivityInfo.COLOR_MODE_HDR
                }
                window.setFormat(android.graphics.PixelFormat.RGBA_1010102)
            }
        }
        onDispose {
            if (hdrActive && window != null) {
                // 用户如果自己调过亮度，就别强行拽回旧值。
                if (window.attributes.screenBrightness ==
                    android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
                ) {
                    window.attributes = window.attributes.apply {
                        screenBrightness = previousBrightness
                        colorMode = previousColorMode
                    }
                }
            }
        }
    }

}
