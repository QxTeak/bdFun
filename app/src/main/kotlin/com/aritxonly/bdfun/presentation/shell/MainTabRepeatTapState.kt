package com.aritxonly.bdfun

/** 按页面及首页频道隔离连击，切页后不沿用上一页的点击时间。 */
internal class MainTabRepeatTapState {
    private var lastKey: String? = null
    private var lastTime = Long.MIN_VALUE

    fun tap(key: String, now: Long): Boolean {
        val refresh = lastKey == key && now >= lastTime && now - lastTime <= 300L
        lastKey = if (refresh) null else key
        lastTime = now
        return refresh
    }

    fun reset() { lastKey = null }
}

internal suspend fun scrollMainPageToTop(state: androidx.compose.foundation.gestures.ScrollableState?) {
    when (state) {
        is androidx.compose.foundation.lazy.LazyListState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.lazy.grid.LazyGridState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState -> state.animateScrollToItem(0)
        is androidx.compose.foundation.ScrollState -> state.animateScrollTo(0)
    }
}
