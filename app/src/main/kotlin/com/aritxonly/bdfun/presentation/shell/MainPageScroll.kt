package com.aritxonly.bdfun

internal suspend fun scrollMainPageToTop(state: androidx.compose.foundation.gestures.ScrollableState?) {
    when (state) {
        is androidx.compose.foundation.lazy.LazyListState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.lazy.grid.LazyGridState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.ScrollState -> state.animateScrollTo(0)
    }
}
