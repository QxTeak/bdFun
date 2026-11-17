package com.aritxonly.bdfun

import com.aritxonly.teak.common.theme.TeakStyle

import com.aritxonly.bdfun.navigateToMessages
import com.aritxonly.bdfun.navigateToLogin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.HomeTopBar
import com.aritxonly.bdfun.ui.HomeTopBarContent
import com.aritxonly.bdfun.ui.HomeTopBarSearch
import com.aritxonly.bdfun.ui.HomeTopBarActions
import com.aritxonly.bdfun.ui.HomeTopBarTabs
import com.aritxonly.bdfun.ui.rememberTeakHaptics
import kotlinx.coroutines.launch

@Composable
internal fun MainHomeBarContent(settings: SettingsBindings, shell: MainShellState, bottomSearch: Boolean = false) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val ui = shell.ui
    val input = shell.searchInput(bottomSearch)
    val session = shell.feeds.session
    val unreadMessageCount = shell.feeds.unreadMessageCount
    val tabPagerState = shell.navigation.pager
    val appScope = shell.navigation.scope
    val searchChrome = shell.chrome.search
    val searchReveal = input.reveal
    val haptics = rememberTeakHaptics()
    // HyperOS 4：分段替换搜索框；哔哩哔哩（官方）与经典：搜索框保留，分段放在它下面一行。
    val tabs = when {
        bottomSearch -> null
        TeakStyle.fromStoredValue(settings.interfaceStyle).layout.hasBottomSearch -> HomeTopBarTabs(
            selected = ui.homeSection,
            onSelect = { haptics.tap(); ui.homeSection = it },
        )
        settings.interfaceStyle == 0 || settings.interfaceStyle == 1 -> HomeTopBarTabs(
            selected = ui.homeSection,
            belowSearch = true,
            onSelect = { haptics.tap(); ui.homeSection = it },
        )
        else -> null
    }
    HomeTopBar(
        content = HomeTopBarContent(
            profile = session,
            unreadCount = unreadMessageCount,
        ),
        search = HomeTopBarSearch(
            active = input.active,
            query = input.query,
            reveal = searchReveal,
            resultsVisible = input.active && input.submitted != null,
        ),
        actions = HomeTopBarActions(
            onQueryChange = { input.changeQuery(it) },
            onSubmitSearch = {
            val word = input.query.trim()
            if (word.isNotEmpty()) {
                settings.addSearchHistory(word)
                input.changeSubmitted(word)
                keyboard?.hide()
            }
        },
            onCancelSearch = {
            if (input.submitted != null) input.changeSubmitted(null) else input.close()
        },
            onClearQuery = { input.changeQuery("") },
            onSearch = {
            searchChrome.reset()
            ui.showSearch = true
            input.changeQuery("")
            input.changeSubmitted(null)
        },
            onOpenSearchPage = {
            // 结果页再点搜索框：回到搜索首页（历史/联想），不要直接改当前结果。
            input.changeSubmitted(null)
            input.changeQuery("")
            keyboard?.show()
        },
            onOpenMessages = { haptics.tap(); navigateToMessages(context) },
            onOpenProfile = {
            if (session == null) navigateToLogin(context) else appScope.launch { tabPagerState.animateScrollToPage(2) }
        },
        ),
        tabs = tabs,
        modifier = Modifier,
    )
}
