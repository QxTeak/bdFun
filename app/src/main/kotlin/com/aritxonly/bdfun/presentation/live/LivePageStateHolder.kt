package com.aritxonly.bdfun.presentation.live

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.LiveDiscoverRepository
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.live.LiveFollower
import com.aritxonly.bdfun.presentation.common.discover.readable
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * 首页「直播」段的状态：关注直播 + 分区 + 分区直播流。
 * 与推荐页一样由 shell 的 TrackTeakPageScroll 接管 gridState（顶栏模糊/玻璃），
 * 下拉刷新状态供 App 层小电视读取。
 *
 * 翻页和首页一样走同一个接口：推荐 tab 用 `/index/feed` 的下一页（PiliPlus 同款），
 * 切到分区后才换成 `second/getList`。
 */
internal class LivePageStateHolder(private val repository: LiveDiscoverRepository) {
    val gridState = LazyGridState()
    /** 我的关注里正在直播的 UP（和动态页头像行同一数据源）。 */
    var followers by mutableStateOf<List<LiveFollower>>(emptyList())
        private set
    var areas by mutableStateOf<List<LiveArea>>(emptyList())
        private set
    var cards by mutableStateOf<List<LiveCard>>(emptyList())
        private set
    var selectedKey by mutableStateOf(0L)
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    /** 外部刷新请求（再次点击底栏当前页）：页面据此先回顶、再拉刷新，与推荐页同一时序。 */
    var refreshRequest by mutableIntStateOf(0)
        private set

    fun requestRefresh() {
        refreshRequest++
    }
    private var selectedParent = 0L
    private var selectedSub = 0L
    private var page = 1
    private var hasMore = true
    private var token = 0

    suspend fun ensureLoaded() {
        if (cards.isNotEmpty() || loading) return
        load()
    }

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        try { load() } finally { refreshing = false }
    }

    suspend fun selectArea(area: LiveArea) {
        if (area.id == selectedKey) return
        selectedKey = area.id
        selectedParent = area.parentAreaId
        selectedSub = area.areaId
        cards = emptyList()
        page = 1
        hasMore = true
        load()
    }

    suspend fun loadMore() {
        if (loading || refreshing || !hasMore) return
        val next = page + 1
        val current = token
        loading = true
        try {
            val more = if (selectedKey == 0L) {
                when (val result = repository.home(next)) {
                    is Outcome.Success -> result.value.cards
                    is Outcome.Failure -> return
                }
            } else {
                when (val result = repository.list(next, selectedParent, selectedSub)) {
                    is Outcome.Success -> result.value
                    is Outcome.Failure -> return
                }
            }
            if (current != token) return
            if (more.isEmpty()) {
                hasMore = false
            } else {
                cards = cards + more
                page = next
            }
        } finally {
            if (current == token) loading = false
        }
    }

    fun retry() { error = null }

    private suspend fun load() {
        token++
        val current = token
        if (cards.isEmpty()) loading = true
        try {
            // 首页、关注直播、分区列表互不依赖，并行拉，首屏只等最慢的一条。
            val (homeResult, followersResult, listResult) = coroutineScope {
                val home = async { repository.home(1) }
                val followers = async { repository.followers() }
                val list = if (selectedKey == 0L) null else async {
                    repository.list(1, selectedParent, selectedSub)
                }
                Triple(home.await(), followers.await(), list?.await())
            }
            var failure: AppError? = null
            val home = when (homeResult) {
                is Outcome.Success -> homeResult.value
                is Outcome.Failure -> { failure = homeResult.error; null }
            }
            val list = when (listResult) {
                null -> null
                is Outcome.Success -> listResult.value
                is Outcome.Failure -> { failure = listResult.error; null }
            }
            if (current != token) return
            if (followersResult is Outcome.Success) {
                followers = followersResult.value.distinctBy { it.roomId }
            }
            if (home != null && home.areas.isNotEmpty()) areas = home.areas
            val first = if (selectedKey == 0L) home?.cards else list
            if (first == null) {
                if (cards.isEmpty()) error = failure?.readable() ?: "加载失败"
                return
            }
            cards = first
            page = 1
            // 推荐 tab 用接口的 has_more；分区列表接口没有该字段，按「这一页非空」继续。
            hasMore = if (selectedKey == 0L) home?.hasMore ?: false else first.isNotEmpty()
            error = null
        } finally {
            if (current == token) loading = false
        }
    }
}
