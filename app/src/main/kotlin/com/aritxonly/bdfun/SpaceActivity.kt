package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigation.readRouteIntent
import com.aritxonly.bdfun.navigation.finishForMissingRoute
import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aritxonly.bdfun.feature.space.presentation.SpacePageViewModel
import com.aritxonly.bdfun.ui.TeakActivityScaffold

class SpaceActivity : ComponentActivity() {
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
            com.aritxonly.bdfun.ui.PhotoViewerRoot {
            TeakActivityScaffold(
                settings = settings,
                onBack = { finish() },
                options = ActivityPageOptions(
                    showTopBar = false,
                    limitContentWidth = false,
                ),
                slots = ActivityPageSlots(
                    overlay = { SpaceTopChrome(chrome, onBack = { finish() }) },
                ),
            ) {
                SpaceScreen(mid = mid, session = AppGraph.session.value, controller = page.currentState(),
                    onTitleChange = {}, chrome = chrome,
                    onBarBlurFractionChange = {}, onBannerDarkChange = { chrome.bannerIsDark = it })
            }
            }
        }
    }
}
