package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.feature.search.application.SearchHistoryEditStateHolder
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
        // 编辑态（长按记录进入）跟着搜索首页一起存活：切到结果页不落盘、退回首页仍在编辑中。
        // 但真删完了要能立刻看到，所以提交后同步刷新 ui.history。
        val historyEdit = remember { SearchHistoryEditStateHolder() }
        val editState by historyEdit.state.collectAsState()
        LaunchedEffect(editState.editing) {
            // 退出编辑态（保存/取消）时草稿作废，界面回到落盘的那份列表。
            if (!editState.editing) ui.history = settings.searchHistory
        }
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
            historyEdit = if (settings.showSearchHistory) HistoryEditSlots(
                editing = editState.editing,
                visible = editState.visible,
                onBegin = { historyEdit.begin(ui.history) },
                onRemove = historyEdit::remove,
                onCancel = historyEdit::cancel,
                onCommit = {
                    historyEdit.commit { removed ->
                        settings.removeSearchHistory(removed)
                        ui.history = settings.searchHistory
                    }
                },
            ) else null,
        )
    } else {
        SearchResults(content, ui, onOpenVideo, onOpenUser, onLoadMore, onLoadMoreUsers, wideResultsBar)
    }
}