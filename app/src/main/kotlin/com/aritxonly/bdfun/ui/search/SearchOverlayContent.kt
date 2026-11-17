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
) {
    val rememberKeyword: (String) -> Unit = { word ->
        settings.addSearchHistory(word)
        ui.history = settings.searchHistory
    }
    if (submitted == null) {
        SearchLanding(
            history = ui.history,
            hot = if (settings.showHotSearch) content.hotKeywords else emptyList(),
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
        )
    } else {
        SearchResults(content, ui, onOpenVideo, onOpenUser, onLoadMore)
    }
}
