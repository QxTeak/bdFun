package com.aritxonly.bdfun

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
    // HyperOS 4 风格：首页顶栏没有搜索框，中间换成「推荐 / 直播 / 番剧」分段。
    // 底栏搜索态（bottomSearch）仍然用原来的搜索输入，不套分段。
    val tabs = if (!bottomSearch && settings.interfaceStyle == 2) {
        HomeTopBarTabs(selected = ui.homeSection, onSelect = { ui.homeSection = it })
    } else {
        null
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
            onOpenMessages = { navigateToMessages(context) },
            onOpenProfile = {
            if (session == null) navigateToLogin(context) else appScope.launch { tabPagerState.animateScrollToPage(2) }
        },
        ),
        tabs = tabs,
        modifier = Modifier,
    )
}
