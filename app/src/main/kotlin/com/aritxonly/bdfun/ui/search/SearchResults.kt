package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.aritxonly.bdfun.core.domain.SearchFilterState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop

@Composable
internal fun SearchResults(
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState, ui: SearchPresentationStateHolder,
    onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
    onLoadMore: () -> Unit = {},
) {
    val tab = ui.tab
    val filterOpen = ui.filterOpen
    val filters = ui.filters
    val onTabChange: (Int) -> Unit = { ui.tab = it }
    val onFilterOpenChange: (Boolean) -> Unit = { ui.filterOpen = it }
    val onFiltersChange: (SearchFilterState) -> Unit = { ui.filters = it }
    // 应用筛选要写回 appliedFilters：搜索 effect 只观察它，写 filters 不会触发重新搜索。
    val onApplyFilters: (SearchFilterState) -> Unit = { ui.appliedFilters = it }
    val presentation = rememberSearchResultsPresentation(tab, onTabChange)
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(presentation.backdrop),
        ) {
            SearchResultPager(presentation, content, onOpenVideo, onOpenUser, onLoadMore)
        }

        // Tab / 筛选按钮固定不动，只跟随页面本身的滚动，不再随列表上移。
        CompositionLocalProvider(LocalTeakBackdrop provides presentation.backdrop) {
            ResultsTabHeader(
                tab = tab,
                onTabChange = onTabChange,
                position = presentation.position,
                filterOpen = filterOpen,
                onFilterOpenChange = onFilterOpenChange,
                filters = filters,
                onFiltersChange = onFiltersChange,
                onApplyFilters = onApplyFilters,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(top = HomeTopBarHeight + 6.dp),
            )
        }
    }
}
