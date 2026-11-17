package com.aritxonly.bdfun.ui

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.data.VideoItem
import kotlinx.coroutines.launch

@Composable
internal fun SearchOverlay(
    settings: SettingsBindings,
    submitted: String?,
    /** 当前输入框里的词；未提交时用它拉搜索联想。 */
    query: String = "",
    reveal: Float,
    onPickKeyword: (String) -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    onOpenUser: (SearchUser) -> Unit,
    controllerFactory: () -> com.aritxonly.bdfun.feature.search.application.SearchStateHolder,
    modifier: Modifier = Modifier,
) {
    val controller = remember { controllerFactory() }
    val content by controller.state.collectAsState()
    val ui = remember { SearchPresentationStateHolder(settings.searchHistory) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LaunchedEffect(Unit) { controller.loadHotKeywords(settings.showHotSearch) }
    LaunchedEffect(query, submitted) { controller.suggest(query, submitted) }
    LaunchedEffect(submitted, ui.appliedFilters) {
        controller.search(submitted, ui.appliedFilters)
        if (submitted == null) {
            ui.tab = 0
            ui.filterOpen = false
        }
    }

    SearchRevealFrame(submitted, reveal, modifier) {
        SearchOverlayContent(settings, submitted, query, ui, content, onPickKeyword, onOpenVideo, onOpenUser,
            onLoadMore = { scope.launch { controller.loadMore() } })
    }
}
