package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.data.VideoItem

@Composable
internal fun SearchOverlayContent(
    settings: SettingsBindings, submitted: String?, query: String, ui: SearchPresentationStateHolder,
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    onPickKeyword: (String) -> Unit, onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
    onLoadMore: () -> Unit = {},
    onLoadMoreUsers: () -> Unit = {},
    landingResetKey: Int = 0,
    wideResultsBar: (@Composable (@Composable () -> Unit) -> Unit)? = null,
) {
    val rememberKeyword: (String) -> Unit = { word ->
        settings.addSearchHistory(word)
        ui.history = settings.searchHistory
    }
    if (submitted == null) {
        SearchLanding(
            history = if (settings.showSearchHistory) ui.history else emptyList(),
            hot = if (settings.showHotSearch) content.hotKeywords else emptyList(),
            // 搜索发现有自己的开关；热搜关掉不影响它。
            discovery = if (settings.showSearchDiscovery) content.discoveryKeywords else emptyList(),
            query = query,
            suggestions = content.suggestions,
            onPickKeyword = { word ->
                rememberKeyword(word)
                onPickKeyword(word)
            },
            onClearHistory = {
                settings.clearSearchHistory()
                ui.history = emptyList()
            },
            resetKey = landingResetKey,
        )
    } else {
        SearchResults(content, ui, onOpenVideo, onOpenUser, onLoadMore, onLoadMoreUsers, wideResultsBar)
    }
}
