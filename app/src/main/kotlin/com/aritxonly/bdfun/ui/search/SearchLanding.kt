package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import com.aritxonly.teak.common.chrome.TeakScrollContainer
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchLanding(
    history: List<String>,
    hot: List<com.aritxonly.bdfun.core.domain.SearchHotWord>,
    discovery: List<String>,
    query: String,
    suggestions: List<String>,
    onPickKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
    /**
     * 每次从外面打开搜索页会变化。搜索页关闭后列表可能还没被回收（收起动画期间），
     * 再打开时会带着上次的位置；这里按这个 key 把列表顶回顶部。
     */
    resetKey: Int = 0,
) {
    val colors = MiuixTheme.colorScheme
    // 内容从顶栏下方开始，但整块可以滚到顶栏下面（顶部留白放 contentPadding 里），
    // 这样滚动时是顶栏的渐进模糊，而不是被一条白边裁断。
    val statusTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    // 热搜/搜索发现是异步到的：数据插到列表最前面时，带 key 的 LazyColumn 会按「原来第一项」
    // 锚定，列表看起来就停在了热搜中间。所以打开页面后、数据到齐时各顶一次。
    LaunchedEffect(resetKey, hot.size, discovery.size, history.size) {
        listState.scrollToItem(0)
    }
    TeakScrollContainer(listState, Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TeakUiStyle.design.contentInset(16.dp),
            // 顶栏高度放进列表留白：列表本身铺满窗口，滚上去的内容由顶栏做渐进模糊。
            top = statusTop + HomeTopBarHeight + 14.dp,
            end = TeakUiStyle.design.contentInset(16.dp),
            bottom = 32.dp,
        ),
    ) {
        if (query.isNotBlank()) {
            searchSuggestionItems(suggestions, colors, onPickKeyword)
            return@LazyColumn
        }
        // 顺序固定：热搜 → 搜索记录 → 搜索发现。
        searchHotItems(hot, colors, onPickKeyword)
        searchHistoryItems(history, colors, onPickKeyword, onClearHistory)
        searchDiscoveryItems(discovery, colors, onPickKeyword)

    }
    }
}
