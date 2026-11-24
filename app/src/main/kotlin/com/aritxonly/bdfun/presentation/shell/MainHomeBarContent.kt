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
internal fun MainHomeBarContent(settings: SettingsBindings, shell: MainShellState, bottomSearch: Boolean = false, tabsCollapseProgress: Float = 0f, resultActions: (@Composable () -> Unit)? = null) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val ui = shell.ui
    val channels = com.aritxonly.bdfun.core.domain.HomeChannelPolicy.ordered(settings.homeChannels, settings.homeChannelOrder)
    val channelIndex = channels.indexOfFirst { it.ordinal == ui.homeSection }.coerceAtLeast(0)
    val input = shell.searchInput(bottomSearch)
    val session = shell.feeds.session
    val tabPagerState = shell.navigation.pager
    val appScope = shell.navigation.scope
    val searchChrome = shell.chrome.search
    val haptics = rememberTeakHaptics()
    val tabs = homeBarTabs(
        settings = settings, channels = channels, channelIndex = channelIndex,
        position = ui.homeSectionPosition, bottomSearch = bottomSearch,
    ) { haptics.tap(); ui.homeSection = channels[it].ordinal }
    // 搜索框里贴视频链接时直接跳视频；普通关键词才进入搜索结果。
    val submitWord = rememberSearchLinkSubmitter(context) { word ->
        settings.addSearchHistory(word)
        input.changeSubmitted(word)
        keyboard?.hide()
    }
    HomeTopBar(
        content = HomeTopBarContent(
            profile = session,
            unreadCount = shell.feeds.unreadMessageCount,
        ),
        search = HomeTopBarSearch(
            active = input.active,
            query = input.query,
            reveal = input.reveal,
            resultsVisible = input.active && input.submitted != null,
        ),
        actions = HomeTopBarActions(
            onQueryChange = { input.changeQuery(it) },
            onSubmitSearch = {
            val word = input.query.trim()
            if (word.isNotEmpty()) submitWord(word)
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
            // 结果页再点搜索框：回到搜索首页继续编辑，保留已输入的关键词（与官方客户端一致），
            // 联想按当前关键词展示；清空只走右侧清除按钮。
            input.changeSubmitted(null)
            keyboard?.show()
        },
            onOpenMessages = { haptics.tap(); navigateToMessages(context) },
            onOpenProfile = {
            if (session == null) navigateToLogin(context) else appScope.launch {
                tabPagerState.animateScrollToPage(
                    2,
                    animationSpec = com.aritxonly.teak.common.navigation.TeakPageSwitchSpec(),
                )
            }
        },
        ),
        tabs = tabs,
        resultActions = resultActions,
        tabsCollapseProgress = tabsCollapseProgress,
        singleRowSearchTabs = com.aritxonly.bdfun.ui.officialHomeBarSingleRow(settings.interfaceStyle),
        modifier = Modifier,
    )
}

/**
 * HyperOS 4：分段替换搜索框；哔哩哔哩（官方）与经典：搜索框保留，分段放在它下面一行。
 * 只做数据拼装，放在主函数外面，避免主函数体过长。
 */
private fun homeBarTabs(
    settings: SettingsBindings,
    channels: List<com.aritxonly.bdfun.core.model.home.HomeChannel>,
    channelIndex: Int,
    position: androidx.compose.runtime.State<Float>,
    bottomSearch: Boolean,
    onSelect: (Int) -> Unit,
): HomeTopBarTabs? = when {
    bottomSearch -> null
    TeakStyle.fromStoredValue(settings.interfaceStyle).layout.hasBottomSearch -> HomeTopBarTabs(
        options = channels.map { it.title }, selected = channelIndex, position = position, onSelect = onSelect,
    )
    settings.interfaceStyle == 0 || settings.interfaceStyle == 1 -> HomeTopBarTabs(
        options = channels.map { it.title }, selected = channelIndex, position = position,
        belowSearch = true, onSelect = onSelect,
    )
    else -> null
}
