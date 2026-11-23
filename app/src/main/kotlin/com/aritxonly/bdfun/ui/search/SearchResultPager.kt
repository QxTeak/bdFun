package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.aritxonly.bdfun.data.SearchUser
import com.aritxonly.bdfun.data.VideoItem

@Composable
internal fun SearchResultPager(
    presentation: SearchResultsPresentation,
    content: com.aritxonly.bdfun.feature.search.application.SearchUiState,
    onOpenVideo: (VideoItem) -> Unit, onOpenUser: (SearchUser) -> Unit,
    onLoadMore: () -> Unit = {},
    onLoadMoreUsers: () -> Unit = {},
) {
    CompositionLocalProvider(
        LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
    ) {
        HorizontalPager(
            state = presentation.pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
            overscrollEffect = null,
        ) { page ->
            SearchResultList(page, presentation, content, onOpenVideo, onOpenUser, onLoadMore, onLoadMoreUsers)
        }
    }
}
