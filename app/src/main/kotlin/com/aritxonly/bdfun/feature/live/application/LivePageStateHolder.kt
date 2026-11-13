package com.aritxonly.bdfun.feature.live.application

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.LiveDiscoverRepository
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard

/**
 * 首页「直播」段的状态：关注直播 + 分区 + 分区直播流。
 * 与推荐页一样由 shell 的 TrackTeakPageScroll 接管 gridState（顶栏模糊/玻璃），
 * 下拉刷新状态供 App 层小电视读取。
 */
internal class LivePageStateHolder(private val repository: LiveDiscoverRepository) {
    val gridState = LazyGridState()
    var following by mutableStateOf<List<LiveCard>>(emptyList())
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
        selectedParent = if (area.parentId > 0L) area.parentId else area.id
        selectedSub = if (area.parentId > 0L) area.id else 0L
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
            when (val result = repository.list(next, selectedParent, selectedSub)) {
                is Outcome.Success -> if (current == token) {
                    val more = result.value
                    if (more.isEmpty()) hasMore = false else {
                        cards = cards + more
                        page = next
                    }
                }
                is Outcome.Failure -> Unit
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
            var failure: AppError? = null
            val home = when (val result = repository.home(1)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> { failure = result.error; null }
            }
            val list = when (val result = repository.list(1, selectedParent, selectedSub)) {
                is Outcome.Success -> result.value
                is Outcome.Failure -> { failure = result.error; null }
            }
            if (current != token) return
            if (home != null) {
                following = home.following
                if (home.areas.isNotEmpty()) areas = home.areas
            }
            // 推荐位优先用首页卡片，避免多打一次分区列表；切分区时才用 list。
            val first = if (selectedKey == 0L && home != null && home.cards.isNotEmpty()) {
                home.cards
            } else {
                list
            }
            if (first == null) {
                if (cards.isEmpty()) error = failure?.readable() ?: "加载失败"
                return
            }
            cards = first
            page = 1
            hasMore = first.isNotEmpty()
            error = null
        } finally {
            if (current == token) loading = false
        }
    }
}

internal fun AppError.readable(): String = when (this) {
    is AppError.RiskControl -> "请求被风控，稍后重试"
    AppError.LoginExpired -> "登录已失效"
    is AppError.Network -> message.ifBlank { "网络错误" }
    is AppError.Parsing -> "数据解析失败"
    is AppError.Unknown -> message.ifBlank { "加载失败" }
}
