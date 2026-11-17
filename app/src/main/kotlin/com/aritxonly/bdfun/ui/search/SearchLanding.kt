package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import com.aritxonly.teak.common.chrome.TeakScrollContainer
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SearchLanding(
    history: List<String>,
    hot: List<String>,
    query: String,
    suggestions: List<String>,
    onPickKeyword: (String) -> Unit,
    onClearHistory: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme

    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    TeakScrollContainer(listState, Modifier.fillMaxSize()) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = TeakUiStyle.design.contentInset(16.dp),
            top = 14.dp,
            end = TeakUiStyle.design.contentInset(16.dp),
            bottom = 32.dp,
        ),
    ) {
        if (query.isNotBlank()) {
            searchSuggestionItems(suggestions, colors, onPickKeyword)
            return@LazyColumn
        }
        searchHistoryItems(history, colors, onPickKeyword, onClearHistory)
        searchHotItems(hot, colors, onPickKeyword)

    }
    }
}
