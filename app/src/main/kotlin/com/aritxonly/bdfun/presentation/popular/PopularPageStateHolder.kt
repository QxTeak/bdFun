package com.aritxonly.bdfun.presentation.popular

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PopularRepository
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.presentation.common.discover.readable

/**
 * 首页「热门」段的状态：单列表分页。
 *
 * 和推荐 / 直播 / 番剧共用同一套下拉刷新：页面持有 PullRefreshSession，
 * 这里只负责 px / dp / 进度回写、刷新中标记，以及「再次点底栏首页」的刷新请求。
 */
internal class PopularPageStateHolder(private val repository: PopularRepository) {
    val gridState = LazyGridState()

    var videos by mutableStateOf<List<VideoItem>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var loadingMore by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var end by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var loadedOnce by mutableStateOf(false)
        private set
    var refreshRequest by mutableIntStateOf(0)
        private set
    /** 下拉位移，供 App 层的小电视读取；与推荐页同一份字段。 */
    var pullOffsetDp by mutableFloatStateOf(0f)
    var pullFraction by mutableFloatStateOf(0f)

    private var page = 0
    private var token = 0

    fun requestRefresh() {
        refreshRequest++
    }

    suspend fun ensureLoaded() {
        if (loadedOnce || loading) return
        reload()
    }

    /** 下拉松手后的刷新：与直播/番剧一致，标记 refreshing 让指示器保持到数据回来。 */
    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        try {
            reload()
        } finally {
            refreshing = false
        }
    }

    /** 拉第一页；刷新与首次加载共用，旧请求的结果由 [token] 丢弃。 */
    suspend fun reload() {
        val current = ++token
        loading = true
        error = null
        try {
            when (val result = repository.page(1)) {
                is Outcome.Success -> {
                    if (current != token) return
                    videos = result.value.videos
                    page = 1
                    end = !result.value.hasMore
                    loadedOnce = true
                }
                is Outcome.Failure -> {
                    if (current != token) return
                    // 已有内容时刷新失败保留旧列表，只有空页才显示错误。
                    if (videos.isEmpty()) error = result.error.readable()
                }
            }
        } finally {
            if (current == token) loading = false
        }
    }

    suspend fun loadMore() {
        if (loading || loadingMore || end) return
        val next = page + 1
        val current = token
        loadingMore = true
        try {
            when (val result = repository.page(next)) {
                is Outcome.Success -> {
                    if (current != token) return
                    // 热门按热度实时变化，翻页时可能回到已出现过的视频，按 bvid 去重。
                    val known = videos.mapTo(HashSet()) { it.bvid }
                    val incoming = result.value.videos.filter { known.add(it.bvid) }
                    videos = videos + incoming
                    page = next
                    end = !result.value.hasMore || incoming.isEmpty()
                    error = null
                }
                is Outcome.Failure -> {
                    if (current == token) error = result.error.readable()
                }
            }
        } finally {
            if (current == token) loadingMore = false
        }
    }

    /** 失败后重试：清掉错误再拉一次，避免错误条一直留在列表底部。 */
    suspend fun retry() {
        error = null
        if (videos.isEmpty()) reload() else loadMore()
    }
}
