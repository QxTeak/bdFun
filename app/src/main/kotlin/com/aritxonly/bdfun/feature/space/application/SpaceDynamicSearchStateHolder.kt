package com.aritxonly.bdfun.feature.space.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.feed.FeedItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class SpaceDynamicSearchState(
    val items: List<FeedItem> = emptyList(), val pages: Int = 0,
    val loading: Boolean = false, val complete: Boolean = false, val error: String? = null,
)

/** 搜索独立遍历动态游标，不改动空间正文的分页位置；失败后从失败页续传。 */
internal class SpaceDynamicSearchStateHolder(
    private val mid: Long, private val repository: SpaceRepository,
    private val currentOwner: () -> Boolean,
) {
    private val mutable = MutableStateFlow(SpaceDynamicSearchState())
    val state = mutable.asStateFlow()
    private var offset = ""
    private val visited = mutableSetOf<String>()
    private var closed = false
    private val active get() = !closed && currentOwner()
    fun close() { closed = true }

    suspend fun loadAll() {
        if (!active || mutable.value.loading || mutable.value.complete) return
        mutable.update { it.copy(loading = true, error = null) }
        try {
            while (active && !mutable.value.complete) {
                currentCoroutineContext().ensureActive()
                val result = repository.dynamics(mid, offset)
                currentCoroutineContext().ensureActive()
                if (!active) return
                when (result) {
                    is Outcome.Failure -> {
                        mutable.update { it.copy(error = result.error.messageOr("动态加载失败，请重试")) }
                        return
                    }
                    is Outcome.Success -> {
                        val page = result.value
                        // 游标异常不能当作全量完成，也不能进入无限请求循环。
                        if (page.hasMore && (page.offset.isBlank() || page.offset == offset || page.offset in visited)) {
                            mutable.update { it.copy(error = "动态分页游标异常，尚未加载全部动态，请重试") }
                            return
                        }
                        visited += offset
                        offset = page.offset
                        mutable.update { it.copy(items = (it.items + page.items).distinctBy { item -> item.id },
                            pages = it.pages + 1, complete = !page.hasMore) }
                    }
                }
                if (!mutable.value.complete) delay(250)
            }
        } finally { mutable.update { it.copy(loading = false) } }
    }
}
