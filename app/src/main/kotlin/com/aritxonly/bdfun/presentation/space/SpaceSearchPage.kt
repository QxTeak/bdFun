package com.aritxonly.bdfun

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import com.aritxonly.teak.common.search.TeakSearchTransitionDefaults
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * 空间页的搜索层。
 *
 * 不再用无动画的独立 Dialog：改成和设置页二级页搜索同一套进场——底层页面
 * 由 Activity 套 `teakSearchUnderlay`，这里只负责搜索面板的同一组淡入/弹簧位移。
 */
@Composable
internal fun SpaceSearchOverlay(runtime: SpaceScreenRuntime, chrome: SpaceChromeState) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val submit = {
        ui.submitted = true
        focus.clearFocus(); keyboard?.hide()
        if (!ui.dynamic) runtime.scope.launch { controller.search() }
        Unit
    }
    BackHandler(enabled = ui.search) { ui.search = false }
    AnimatedVisibility(
        visible = ui.search,
        modifier = Modifier.fillMaxSize(),
        enter = fadeIn(tween(200, delayMillis = 50)) +
            slideInVertically(
                spring(
                    dampingRatio = TeakSearchTransitionDefaults.SpringDampingRatio,
                    stiffness = TeakSearchTransitionDefaults.SpringStiffness,
                ),
            ) { it / 12 },
        exit = fadeOut(tween(150)),
    ) {
        // 搜索层盖住头图后，状态栏图标要按页面底色重新取对比度（原来是独立窗口自己设的）。
        val view = LocalView.current
        val lightBars = MiuixTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            view.context.findHostActivity()?.let {
                androidx.core.view.WindowCompat.getInsetsController(it.window, it.window.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        // 和原独立窗口一致：不采样底层空间页背景，避免把头图带进搜索材质。
        CompositionLocalProvider(LocalTeakBackdrop provides null) {
            Column(
                Modifier.fillMaxSize()
                    .background(MiuixTheme.colorScheme.background)
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                HomeTopBar(HomeTopBarContent(null),
                    HomeTopBarSearch(active = true, query = state.query, reveal = 1f,
                        resultsVisible = ui.submitted, placeholder = "搜索此空间的视频、动态"),
                    HomeTopBarActions(onSearch = {}, onOpenMessages = {}, onOpenProfile = {},
                        onQueryChange = controller::query, onSubmitSearch = submit,
                        onCancelSearch = { ui.search = false },
                        onClearQuery = { controller.query(""); ui.submitted = false },
                        onOpenSearchPage = { ui.submitted = false }))
                Box(Modifier.fillMaxWidth().height(48.dp), contentAlignment = Alignment.Center) {
                    TeakPagerTabs(options = listOf("视频", "动态"), selected = if (ui.dynamic) 1 else 0,
                        onSelect = { ui.dynamic = it == 1 }, itemWidth = 72.dp)
                }
                val name = runtime.content.snapshot.profile.space?.profile?.name.orEmpty()
                Text(if (name.isBlank()) "仅搜索此空间" else "搜索 $name 的空间",
                    color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp))
                if (ui.dynamic) SpaceDynamicSearchResults(runtime, state.query)
                else SpaceVideoSearchResults(runtime)
            }
        }
    }
}
