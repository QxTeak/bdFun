package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import com.aritxonly.bdfun.feature.library.application.*

/**
 * 媒体库搜索栏：直接复用首页点搜索后的顶栏（同一套玻璃胶囊、返回键、字号和清除按钮），
 * 只把占位文案换成当前页的，例如「搜索收藏」。以前这里另画了一条扁平灰胶囊，
 * 和首页搜索框明显不是一套，切页时观感很割裂。
 */
@Composable
internal fun LibrarySearchHeader(
    title: String,
    state: LibrarySearchState,
    events: LibrarySearchEvents,
    onBack: () -> Unit,
) {
    HomeTopBar(
        content = HomeTopBarContent(profile = null),
        search = HomeTopBarSearch(
            active = true,
            query = state.draft,
            placeholder = "搜索$title",
        ),
        actions = HomeTopBarActions(
            onSearch = {},
            onOpenMessages = {},
            onOpenProfile = {},
            onQueryChange = events::changeDraft,
            onSubmitSearch = events::submit,
            onCancelSearch = onBack,
            onClearQuery = events::clearInput,
        ),
    )
}
