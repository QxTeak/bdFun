package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.createRouteIntent
import com.aritxonly.bdfun.core.model.settings.AUTO_CDN_ID
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.aritxonly.bdfun.data.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {
    private lateinit var settings: SettingsStore

    private lateinit var incomingLinks: com.aritxonly.bdfun.core.application.IncomingVideoLinkStateHolder

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // App 退到后台 / 系统内存紧张时释放图片内存缓存；首页列表本身保留。
        if (level >= android.content.ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            coil.Coil.imageLoader(this).memoryCache?.clear()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.aritxonly.bdfun.data.LogCollector.init(applicationContext)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        incomingLinks = AppGraph.incomingVideoLinks()
        settings = AppGraph.settings
        // 默认保留玻璃录制的 60Hz 预算；0 表示由系统自行选择。
        lifecycleScope.launch {
            settings.state.map { settings.repository.appearance.feedRefreshRate }
                .distinctUntilChanged().collect { rate ->
                window.attributes = window.attributes.apply {
                    preferredRefreshRate = rate.toFloat()
                }
            }
        }
        // 自动 CDN 在冷启动先沿用上次结果，同时在后台重新测速；不阻塞首屏和播放。
        if (settings.cdnService == AUTO_CDN_ID) {
            lifecycleScope.launch(Dispatchers.IO) {
                AppGraph.refreshAutomaticCdn()
            }
        }
        if (savedInstanceState == null) checkUpdateOnStartup()
        handleIntent(intent)
        setContent {
            // 图片查看器挂在窗口最外层，才能盖住底栏、顶栏和状态栏（和 Telegram 一致）。
            com.aritxonly.bdfun.ui.PhotoViewerRoot(animationsEnabled = settings.animationsEnabled) {
                androidx.compose.foundation.layout.Box {
                    BdFunApp(settings = settings)
                    com.aritxonly.bdfun.ui.SplashWallpaper(settings.splashWallpaper)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 只在这次进程的第一次前台读取剪贴板：旋转重建、从视频页返回都不再重复弹跳转。
        if (!clipboardChecked) {
            clipboardChecked = true
            handleClipboard()
        }
    }

    private fun handleIntent(intent: Intent?) {
        val raw = intent?.data?.toString()
            ?: intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: return
        openLink(raw)
    }

    private fun handleClipboard() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
        val text = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
        incomingLinks.clipboard(text)?.let(::openLink)
    }

    private fun openLink(text: String) {
        incomingLinks.begin(text)?.let(::openLink)
    }

    private fun openLink(request: com.aritxonly.bdfun.core.application.PendingVideoLink) {
        lifecycleScope.launch {
            val route = incomingLinks.resolve(request) ?: return@launch
            startActivity(createRouteIntent(this@MainActivity, route))
        }
    }

    override fun onDestroy() {
        if (::incomingLinks.isInitialized) incomingLinks.close()
        super.onDestroy()
    }

    private companion object {
        /** 进程级：Activity 旋转重建不重复读剪贴板；进程重启后重新检查一次。 */
        var clipboardChecked = false
    }
}
