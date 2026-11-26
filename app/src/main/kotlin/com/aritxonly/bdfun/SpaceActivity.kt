package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute
import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aritxonly.teak.common.search.teakSearchUnderlay
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aritxonly.bdfun.feature.space.presentation.SpacePageViewModel
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class SpaceActivity : BdFunActivity() {
    private var lightStatusIcons = false

    internal fun syncSpaceStatusIcons(light: Boolean = lightStatusIcons) {
        lightStatusIcons = light
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = !light
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) syncSpaceStatusIcons()
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // MIUI may reset contrast on fold/rotation even when this Activity is retained.
        window.decorView.post { syncSpaceStatusIcons() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val mid = (readRouteIntent(intent) as? com.aritxonly.bdfun.core.navigation.Route.Space)?.mid
        if (mid == null || mid <= 0) {
            finishForMissingRoute("用户标识")
            return
        }
        val page = ViewModelProvider(this, viewModelFactory {
            initializer { SpacePageViewModel(AppGraph.spacePageResources(mid)) }
        })[SpacePageViewModel::class.java]
        setContent {
            val settings = AppGraph.settings
            val chrome = remember { SpaceChromeState() }
            val runtime = rememberSpaceScreenRuntime(mid, AppGraph.session.value, page.currentState())
            // 和设置页搜索同一套：底层内容上移 + 模糊 + 渐隐，搜索面板弹簧淡入。
            val animations = settings.animationsEnabled
            val searchProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (chrome.tools.search) 1f else 0f,
                animationSpec = if (animations) {
                    com.aritxonly.teak.common.search.TeakSearchTransitionDefaults.spec()
                } else {
                    androidx.compose.animation.core.tween(0)
                },
                label = "space-search-underlay",
            )
            val searchAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (chrome.tools.search) 0f else 1f,
                animationSpec = androidx.compose.animation.core.tween(
                    if (animations) com.aritxonly.teak.common.search.TeakSearchTransitionDefaults.UnderlayAlphaMs else 0,
                ),
                label = "space-search-underlay-alpha",
            )
            com.aritxonly.teak.common.navigation.TeakMorphMenuProvider {
            val videoMorph = com.aritxonly.bdfun.ui.rememberVideoCardMorphMenuHandler(com.aritxonly.bdfun.ui.rememberVideoShareMorphItems())
            androidx.compose.runtime.CompositionLocalProvider(com.aritxonly.bdfun.ui.LocalVideoCardMorphMenu provides videoMorph) {
            com.aritxonly.bdfun.ui.PhotoViewerRoot(animationsEnabled = settings.animationsEnabled) {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    showTopBar = false,
                    limitContentWidth = false,
                ),
                // 搜索层是整页覆盖，唱片不该浮在搜索页上。
                disc = com.aritxonly.bdfun.ui.ActivityDiscOptions(
                    showBackgroundDisc = !chrome.tools.search,
                ),
                slots = ActivityPageSlots(
                    overlay = {
                        if (searchProgress < 0.999f) {
                            androidx.compose.foundation.layout.Box(
                                androidx.compose.ui.Modifier
                                    .fillMaxSize()
                                    .teakSearchUnderlay(searchProgress, searchAlpha),
                            ) { SpaceTopChrome(chrome, onBack = { finish() }) }
                        }
                        SpaceSearchOverlay(runtime, chrome)
                        SpaceToolsDialogs(runtime, chrome)
                    },
                    menuOverlay = { SpaceToolsMenu(runtime, chrome) },
                ),
            ) {
                androidx.compose.foundation.layout.Box(
                    androidx.compose.ui.Modifier
                        .fillMaxSize()
                        .teakSearchUnderlay(searchProgress, searchAlpha),
                ) {
                    SpaceScreen(runtime = runtime,
                        onTitleChange = {}, chrome = chrome,
                        onBarBlurFractionChange = {}, onBannerDarkChange = { chrome.bannerIsDark = it })
                }
            }
            }
            }
            }
        }
    }
}
