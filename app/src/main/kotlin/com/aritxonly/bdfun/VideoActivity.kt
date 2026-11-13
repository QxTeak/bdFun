package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute
import com.aritxonly.bdfun.navigateToVideo
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.aritxonly.bdfun.ui.LocalVideoPageState
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/** 视频详情：独立 Activity；评论页留在本 Activity 内，避免播放器被销毁。 */
class VideoActivity : ComponentActivity() {
    private val pageState by lazy { AppGraph.videoPageState(lifecycleScope) }
    private val VideoPageBus get() = pageState
    private val PlayerUiState get() = pageState.playerUi
    private val VideoOrientationLock get() = pageState.orientation


    // ---- 自己监听传感器决定横竖屏 ----
    private var splitSystemBars = false
    private var pipEntered = false
    private val windowState = com.aritxonly.bdfun.ui.VideoActivityWindowStateHolder()
    private var pipMode by windowState.pipMode
    private var openingAnotherPage = false

    private fun syncVideoSystemBars(split: Boolean = splitSystemBars) {
        splitSystemBars = split
        val dark = when (AppGraph.settings.darkMode) {
            1 -> false
            2 -> true
            else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = split && !dark
    }

    private var orientationListener: android.view.OrientationEventListener? = null
    private var pendingLandscape: Boolean? = null
    private var pendingSince = 0L

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    /**
     * 系统是否锁定了方向（下拉快捷开关里的「方向锁定」/ 设置里的「自动旋转屏幕」关闭）。
     * 锁定后即使 App 内的「自动旋转」是开的也不能转。
     */
    private fun systemRotationLocked(): Boolean = runCatching {
        Settings.System.getInt(
            contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            1,
        ) == 0
    }.getOrDefault(false)

    private fun autoRotateAllowed(): Boolean =
        AppGraph.settings.autoRotate && !systemRotationLocked()

    /** 上一次真正生效的设备姿态（0 竖屏 / 1 横屏）：只有姿态真的翻转才驱动转屏。 */
    private var lastQuadrant: Int = -1

    private fun startOrientationTracking() {
        if (orientationListener != null) return
        lastQuadrant = if (
            resources.configuration.orientation ==
            android.content.res.Configuration.ORIENTATION_LANDSCAPE
        ) 1 else 0
        orientationListener = object : android.view.OrientationEventListener(
            this,
            android.hardware.SensorManager.SENSOR_DELAY_UI,
        ) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                // 每次方向变化都重新读：用户可能中途打开 / 关闭了方向锁定
                if (!PlayerUiState.fullscreen || !autoRotateAllowed() || VideoOrientationLock.locked) return
                // 判定：横屏要转到离水平 12° 以内（比上一版 18° 更严格）
                val landscape = orientation in 78..102 || orientation in 258..282
                val portrait =
                    orientation in 0..24 || orientation in 156..204 || orientation in 336..359
                val quadrant = when {
                    landscape -> 1
                    portrait -> 0
                    else -> return
                }
                // 姿态没变就不动：这样「竖屏时点全屏按钮」不会被传感器立刻扳回竖屏
                if (quadrant == lastQuadrant) {
                    pendingLandscape = null
                    return
                }
                val target = quadrant == 1
                if (pendingLandscape != target) {
                    pendingLandscape = target
                    pendingSince = System.currentTimeMillis()
                    return
                }
                // 姿态稳定保持 220ms 就转屏：再长会让「手机转过去 App 半天不跟」
                if (System.currentTimeMillis() - pendingSince < 220) return
                lastQuadrant = quadrant
                val wanted = if (target) {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }
                if (requestedOrientation != wanted) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d(
                            "ROTDBG",
                            "apply requestedOrientation=$wanted (angle=$orientation)",
                        )
                    }
                    requestedOrientation = wanted
                }
            }
        }.also { it.enable() }
        if (BuildConfig.DEBUG) {
            android.util.Log.d("ROTDBG", "orientation listener enabled")
        }
    }

    private fun stopOrientationTracking() {
        orientationListener?.disable()
        orientationListener = null
        pendingLandscape = null
    }

    override fun onResume() {
        super.onResume()
        openingAnotherPage = false
        if (!isInPictureInPictureMode) pipEntered = false
        // Split panes use the page theme under system bars; stacked/fullscreen video uses light icons.
        runCatching {
            syncVideoSystemBars()
        }
        if (!PlayerUiState.fullscreen) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        if (autoRotateAllowed()) {
            startOrientationTracking()
        } else {
            stopOrientationTracking()

        }
    }

    override fun onPause() {
        super.onPause()
        stopOrientationTracking()
    }

    /** 退出视频：若设置选择「画中画」，先进入系统画中画；否则正常关闭并交给唱片。 */
    private fun exitVideoPage() {
        val s = AppGraph.settings
        if (s.stopOnVideoPageExit) {
            com.aritxonly.bdfun.ui.BackgroundPlayback.cancelHandOff()
            VideoPageBus.pauseFor(this)
            finish()
            return
        }
        if (s.showDiscAfterVideo && s.exitVideoControl == 0) {
            val ok = runCatching {
                val params = android.app.PictureInPictureParams.Builder()
                    .setAspectRatio(android.util.Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            }.getOrDefault(false)
            if (ok) return
        }
        finish()
    }

    // Home/background is distinct from navigating out of the video page.
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppGraph.settings.stopOnVideoPageExit && !isFinishing && !openingAnotherPage &&
            !isInPictureInPictureMode && com.aritxonly.bdfun.ui.BackgroundPlayback.playing) {
            val entered = runCatching { enterPictureInPictureMode(android.app.PictureInPictureParams.Builder()
                .setAspectRatio(android.util.Rational(16, 9)).build()) }.getOrDefault(false)
            if (entered) pipEntered = true else VideoPageBus.pauseFor(this)
        }
    }

    override fun startActivityForResult(intent: android.content.Intent, requestCode: Int, options: Bundle?) {
        openingAnotherPage = true
        try { super.startActivityForResult(intent, requestCode, options) }
        catch (error: Exception) { openingAnotherPage = false; throw error }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: android.content.res.Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        pipMode = isInPictureInPictureMode
        if (isInPictureInPictureMode) pipEntered = true
    }

    override fun onStop() {
        // Dismissing PiP stops the Activity without guaranteeing immediate destruction.
        if (AppGraph.settings.stopOnVideoPageExit && (pipEntered || isFinishing) && !isChangingConfigurations) {
            com.aritxonly.bdfun.ui.BackgroundPlayback.cancelHandOff()
            VideoPageBus.pauseFor(this)
        }
        super.onStop()
    }

    override fun onDestroy() {
        // 只有真正退出这个视频页才清相关推荐；从相关推荐进另一个视频时
        // 这个 Activity 只是 stop、不会 finish，所以返回时不会重载。
        pageState.readCache.close(if (isFinishing) VideoPageBus.currentBvid else null)
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // 从评论/空间等页面返回或转场结束时，补一次图标明暗同步。
            runCatching {
                syncVideoSystemBars()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!PlayerUiState.fullscreen) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_USER
        if (BuildConfig.DEBUG) {
            android.util.Log.d(
                "ROTDBG",
                "onConfigChanged orientation=${newConfig.orientation} rotation=${display?.rotation} " +
                    "size=${resources.displayMetrics.widthPixels}x${resources.displayMetrics.heightPixels} " +
                    "reqOri=$requestedOrientation",
            )
        }
    }

    /**
     * 视频页的状态栏样式：
     * - 沉浸：状态栏透明，视频才真正铺到状态栏下面
     * - 非沉浸：状态栏保持黑色（与旧版一致）
     */
    private fun applyStatusBarStyle(immersive: Boolean) {
        enableEdgeToEdge(
            // Content paints each pane to the window edges; system bars must not add a solid strip.
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT,
            ) { resources ->
                when (AppGraph.settings.darkMode) {
                    1 -> false
                    2 -> true
                    else -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                        android.content.res.Configuration.UI_MODE_NIGHT_YES
                }
            },
        )
        window.isNavigationBarContrastEnforced = false
    }

    @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        // 任何方式新开视频页，都先停掉后台播放器，避免两路声音。
        com.aritxonly.bdfun.ui.BackgroundPlayback.stopDetached()
        applyStatusBarStyle(AppGraph.settings.videoImmersiveStatusBar)
        if (android.os.Build.VERSION.SDK_INT >= 33 && !AppGraph.settings.notifAsked) {
            val granted = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                AppGraph.settings.notifAsked = true
                runCatching {
                    requestPermissions(
                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                        1001,
                    )
                }
            }
        }
        val isLoggedIn = AppGraph.loginStatus::isLoggedIn
        val settings = AppGraph.settings
        val route = readRouteIntent(intent) as? com.aritxonly.bdfun.core.navigation.Route.Video
        val requestedBvid = route?.bvid?.takeIf { it.isNotBlank() } ?: intent?.getStringExtra("bvid")
        val requestedAid = route?.aid ?: intent?.getLongExtra("aid", 0L) ?: 0L
        if (route?.snapshot == null && requestedBvid.isNullOrBlank() && requestedAid <= 0L) {
            finishForMissingRoute("视频标识")
            return
        }
        setContent {
            com.aritxonly.bdfun.ui.PhotoViewerRoot {
            androidx.compose.runtime.CompositionLocalProvider(LocalVideoPageState provides pageState,
                com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings provides settings,
                com.aritxonly.bdfun.ui.LocalRichTextDependencies provides AppGraph.richTextDependencies) {
            com.aritxonly.bdfun.ui.VideoActivityContent(isLoggedIn, settings, route, requestedBvid, requestedAid, pipMode,
                com.aritxonly.bdfun.ui.VideoActivityHostActions(
                    onBack = ::exitVideoPage,
                    finish = ::finish,
                    applyStatusBarStyle = ::applyStatusBarStyle,
                    resyncSystemBars = { syncVideoSystemBars() },
                    syncSystemBars = { syncVideoSystemBars(it) },
                    openVideo = { navigateToVideo(this@VideoActivity, it) },
                    openedFromVideoPage = { intent?.getBooleanExtra("fromVideoPage", false) == true },
                    hasWindow = { window != null },
                ))
            }
            }
        }
    }
}
