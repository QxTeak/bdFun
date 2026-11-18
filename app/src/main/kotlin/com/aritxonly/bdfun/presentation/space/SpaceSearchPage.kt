package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.aritxonly.bdfun.ui.*
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.navigation.TeakPagerTabs
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text

@Composable
internal fun SpaceSearchPage(runtime: SpaceScreenRuntime, ui: SpaceToolsUiState) {
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
    Dialog(onDismissRequest = { ui.search = false }, properties = DialogProperties(
        usePlatformDefaultWidth = false, decorFitsSystemWindows = false,
    )) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        val lightBars = MiuixTheme.colorScheme.background.luminance() > 0.5f
        SideEffect {
            window?.let {
                it.setDimAmount(0f)
                androidx.core.view.WindowCompat.getInsetsController(it, it.decorView).apply {
                    isAppearanceLightStatusBars = lightBars
                    isAppearanceLightNavigationBars = lightBars
                }
            }
        }
        // 独立窗口不采样底层空间页的背景，避免把头图带进搜索材质。
        CompositionLocalProvider(LocalTeakBackdrop provides null) {
            Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background)
                .navigationBarsPadding().imePadding()) {
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
