package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow

@Composable
internal fun FeedPaginationEffects(state: FeedState, loggedIn: Boolean) {
    LaunchedEffect(state.type, state.filterMid) { state.listState.scrollToItem(0) }
    LaunchedEffect(state, state.type, state.filterMid) {
        snapshotFlow {
            state.listState.layoutInfo.visibleItemsInfo.maxOfOrNull { it.index } to state.listState.layoutInfo.totalItemsCount
        }.collect { (last, count) ->
            if (count > 0 && last != null && last >= count - 2 && state.error == null) {
                state.loadMore(loggedIn)
            }
        }
    }

}
