package com.aritxonly.bdfun.feature.discover.application

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import com.aritxonly.bdfun.core.model.live.LiveArea
import com.aritxonly.bdfun.core.model.live.LiveCard
import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import com.aritxonly.bdfun.data.BiliApi

/**
 * 首页「直播」段的状态：关注直播 + 分区 + 分区直播流。
 * 刷新/加载失败只写到 error，不改动已有列表，界面按「空态 + 重试」处理。
 */
internal class LivePageStateHolder(private val api: BiliApi) {
    var following by mutableStateOf<List<LiveCard>>(emptyList())
        private set
    var areas by mutableStateOf<List<LiveArea>>(emptyList())
        private set
    var cards by mutableStateOf<List<LiveCard>>(emptyList())
        private set
    var selectedKey by mutableStateOf(0L)
        private set
    private var selectedParent = 0L
    private var selectedSub = 0L
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    private var page = 1
    private var hasMore = true
    var loadMoreFailed by mutableStateOf(false)
        private set
    private var token = 0

    suspend fun ensureLoaded() {
        if (cards.isNotEmpty() || loading) return
        load(reset = true)
    }

    suspend fun refresh() {
        if (refreshing) return
        refreshing = true
        try { load(reset = true) } finally { refreshing = false }
    }

    suspend fun selectArea(area: LiveArea) {
        val key = area.id
        if (key == selectedKey) return
        selectedKey = key
        selectedParent = if (area.parentId > 0L) area.parentId else area.id
        selectedSub = if (area.parentId > 0L) area.id else 0L
        cards = emptyList()
        page = 1
        hasMore = true
        load(reset = true)
    }

    suspend fun loadMore() {
        if (loading || refreshing || !hasMore) return
        val next = page + 1
        val current = token
        loading = true
        loadMoreFailed = false
        try {
            val more = api.liveRecommend(next, selectedParent, selectedSub)
            if (current != token) return
            if (more.isEmpty()) { hasMore = false } else {
                cards = cards + more
                page = next
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            loadMoreFailed = true
        } finally {
            if (current == token) loading = false
        }
    }

    fun retry() { error = null }

    /** 一级分区 + 「娱乐」的子分区（聊天室 / 颜值等），顺序与 B 站直播页一致。 */
    private suspend fun loadAreas(): List<LiveArea> {
        val parents = runCatching { api.liveAreaList(0L) }.getOrDefault(emptyList())
        if (parents.isEmpty()) return emptyList()
        val sub = parents.firstOrNull { it.name.contains("娱乐") }?.let { parent ->
            runCatching { api.liveAreaList(parent.id) }.getOrDefault(emptyList())
        }.orEmpty()
        return parents + sub
    }

    private suspend fun load(reset: Boolean) {
        token++
        val current = token
        if (cards.isEmpty()) loading = true
        loadMoreFailed = false
        try {
            if (areas.isEmpty()) {
                areas = loadAreas()
            }
            val followingNow = runCatching { api.liveFollowing(1) }.getOrDefault(emptyList())
            val list = api.liveRecommend(1, selectedParent, selectedSub)
            if (current != token) return
            following = followingNow
            cards = list
            page = 1
            hasMore = list.isNotEmpty()
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != token) return
            if (cards.isEmpty()) error = e.message.orEmpty().ifBlank { "加载失败" }
        } finally {
            if (current == token) loading = false
        }
    }
}

/** 首页「番剧」段的状态：最近追番 + 推荐。 */
internal class BangumiPageStateHolder(private val api: BiliApi) {
    var following by mutableStateOf<List<BangumiCard>>(emptyList())
        private set
    var cards by mutableStateOf<List<BangumiCard>>(emptyList())
        private set
    var loading by mutableStateOf(false)
        private set
    var refreshing by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var pullFraction by mutableFloatStateOf(0f)
    var pullOffsetDp by mutableFloatStateOf(0f)
    private var page = 1
    private var hasMore = true
    var loadMoreFailed by mutableStateOf(false)
        private set
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

    suspend fun loadMore() {
        if (loading || refreshing || !hasMore) return
        val next = page + 1
        val current = token
        loading = true
        loadMoreFailed = false
        try {
            val more = api.pgcIndex(next)
            if (current != token) return
            if (more.isEmpty()) { hasMore = false } else {
                cards = cards + more
                page = next
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            loadMoreFailed = true
        } finally {
            if (current == token) loading = false
        }
    }

    fun retry() { error = null }

    private suspend fun load() {
        token++
        val current = token
        if (cards.isEmpty()) loading = true
        loadMoreFailed = false
        try {
            val followed = runCatching { api.pgcFollowing(1) }.getOrDefault(emptyList())
            val list = api.pgcIndex(1)
            if (current != token) return
            following = followed
            cards = list
            page = 1
            hasMore = list.isNotEmpty()
            error = null
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (current != token) return
            if (cards.isEmpty()) error = e.message.orEmpty().ifBlank { "加载失败" }
        } finally {
            if (current == token) loading = false
        }
    }
}

/** 两个页面共用的网格滚动状态缓存，避免切段/返回时丢位置。 */
internal object DiscoverGridStates {
    var live: LazyGridState? = null
    var bangumi: LazyGridState? = null
}
