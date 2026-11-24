package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.teak.common.chrome.LocalTeakPageScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.SearchOverlay
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun MainSearchLayer(settings: SettingsBindings, shell: MainShellState, bottomSearch: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val input = shell.searchInput(bottomSearch)
    val searchChrome = shell.chrome.search
    val searchBackdrop = shell.backdrops.search
    val searchReveal = input.reveal
    // 搜索页收起后可能还留在组合里（收起动画期间），再打开会带着上次的列表位置；
    // 每次从关到开递增一次，让搜索首页自己把列表顶回顶部。
    val pickKeyword = rememberSearchLinkSubmitter(context) { word ->
        input.changeQuery(word)
        settings.addSearchHistory(word)
        input.changeSubmitted(word)
        keyboard?.hide()
    }
    var openTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(input.active) {
        if (input.active) openTick++
    }
    // 搜索页：搜索框保持不动，下面整块换成纯白页（搜索记录 + 热搜 / 结果）
    if (input.active || searchReveal > 0.01f) {
        // 搜索页复刻首页结构：内容先录进 searchBackdrop，
        // 顶栏的渐变模糊和筛选菜单都采样同一块。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(searchBackdrop),
        ) {
            CompositionLocalProvider(
                LocalTeakBackdrop provides searchBackdrop,
                LocalTeakPageScrollState provides searchChrome,
                com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress provides searchChrome.materialProgress,
            ) {
                SearchOverlay(
                    settings = settings,
                    wideResultsBar = if (com.aritxonly.bdfun.ui.useSingleRowSearchResults()) {
                        { trailing -> MainHomeBarContent(settings, shell, bottomSearch, resultActions = trailing) }
                    } else null,
                    controllerFactory = shell.searchFactory,
                    submitted = input.submitted,
                    reveal = searchReveal,
                    query = input.query,
                    onPickKeyword = pickKeyword,
                    onOpenVideo = { navigateToVideo(context, it) },
                    onOpenUser = { navigateToSpace(context, it.mid) },
                    landingResetKey = openTick,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }

}
