package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.ui.syncSystemBarIcons

/**
 * 动态详情：设置页同款外壳（大标题 / 折叠小标题 / 顶部渐进式模糊 / 玻璃返回键），
 * 正文是动态卡片，下面是和视频评论区完全同源的评论列表；底部复用视频评论页的回复栏。
 */
class DynamicDetailActivity : ComponentActivity() {
    /** 状态栏背景是页面本身（浅色/深色），图标明暗必须跟着实际页面底色走。 */
    private fun syncStatusBarIcons() {
        val systemDark = (resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val appDark = when (AppGraph.settings.darkMode) {
            1 -> false
            2 -> true
            else -> systemDark
        }
        syncSystemBarIcons(appDark)
    }

    override fun onResume() {
        super.onResume()
        // 从图库/其他页面回来时系统会重置图标明暗，重新同步
        syncStatusBarIcons()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // 转场/分屏后系统可能重置图标明暗，重新获得焦点时再同步一次。
        if (hasFocus) {
            syncStatusBarIcons()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.ensure(applicationContext)
        // 动态详情是沉浸式渐变页：状态栏透明，让页面背景透上去。
        // 必须自己接管，否则 One UI 等系统会按主题默认值画成白条。
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(
                android.graphics.Color.TRANSPARENT,
            ),
        )
        syncStatusBarIcons()
        val dynamic = (readRouteIntent(intent) as? com.aritxonly.bdfun.core.navigation.Route.Dynamic)?.snapshot
        if (dynamic == null) {
            finishForMissingRoute("动态详情数据")
            return
        }
        setContent {
            com.aritxonly.bdfun.ui.PhotoViewerRoot {
                DynamicDetailContent(dynamic, onBack = { finish() })
            }
        }

    }
}
