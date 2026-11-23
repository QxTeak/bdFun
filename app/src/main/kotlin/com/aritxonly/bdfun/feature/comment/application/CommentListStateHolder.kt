package com.aritxonly.bdfun.feature.comment.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.comment.CommentItem
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class CommentListState(
    val items: List<CommentItem> = emptyList(), val total: Int = 0, val page: Int = 0,
    val loading: Boolean = true, val loadingMore: Boolean = false, val end: Boolean = false,
    val error: AppError? = null, val query: CommentQuery,
)

/** 一页评论被屏蔽干净时最多再往后补取几页，防止极端情况下无限翻页。 */
private const val FILTERED_PAGE_SKIPS = 3

/** 主评论与楼中楼共用分页规则，查询版本隔离排序、评论区与根评论切换。 */
internal class CommentListStateHolder(
    private val repository: CommentContentRepository,
    query: CommentQuery,
    /** 每次拉取时读取当前屏蔽设置；关闭时策略是空转的，不改动任何数据。 */
    private val blockConfig: () -> CommentAiBlockConfig = { CommentAiBlockConfig() },
) {
    private val mutableState = MutableStateFlow(CommentListState(query = query))
    val state = mutableState.asStateFlow()
    private var revision = 0L
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        revision++
        mutableState.update { it.copy(loading = false, loadingMore = false) }
    }

    suspend fun reset(query: CommentQuery = mutableState.value.query) {
        if (closed) return
        revision++
        mutableState.value = CommentListState(query = query)
        reload()
    }

    suspend fun reload() {
        if (closed) return
        val version = ++revision
        mutableState.update { it.copy(loading = true, loadingMore = false, error = null, end = false) }
        try { fetch(1, version) }
        finally { if (version == revision) mutableState.update { it.copy(loading = false) } }
    }

    /** 保留 main 的可见项触发条件，但每次滚动读取当前状态，避免捕获首次组合的 loading 值。 */
    fun shouldLoadMore(last: Int, count: Int): Boolean {
        val current = mutableState.value
        return !closed && current.items.isNotEmpty() && !current.end && !current.loading && !current.loadingMore && last >= count - 1
    }

    suspend fun loadMore() {
        val before = mutableState.value
        if (closed || before.loading || before.loadingMore || before.end) return
        val version = revision
        mutableState.update { it.copy(loadingMore = true, error = null) }
        try { fetch(before.page + 1, version) }
        finally { if (version == revision) mutableState.update { it.copy(loadingMore = false) } }
    }

    private suspend fun fetch(page: Int, version: Long) {
        val query = mutableState.value.query
        val policy = CommentAiBlockPolicy(blockConfig())
        val replace = page == 1
        var target = page
        var skipped = 0
        while (true) {
            val result = repository.page(query, target)
            currentCoroutineContext().ensureActive()
            if (closed || version != revision) return
            if (result is Outcome.Failure) {
                mutableState.update { it.copy(error = result.error) }
                return
            }
            val pageData = (result as Outcome.Success).value
            val filtered = policy.filter(pageData.items)
            // 整页都是 AI 评论时继续往后取，否则用户只会看到空评论区；上限避免极端情况下空转翻页。
            if (filtered.isEmpty() && pageData.items.isNotEmpty() && !pageData.isEnd && skipped < FILTERED_PAGE_SKIPS) {
                target++
                skipped++
                continue
            }
            mutableState.update {
                val items = if (replace) filtered else (it.items + filtered).distinctBy { item -> item.rpid }
                it.copy(items = items, total = pageData.total.takeIf { count -> count > 0 } ?: items.size,
                    page = target, end = pageData.isEnd || filtered.isEmpty())
            }
            return
        }
    }

    fun applyLike(rpid: Long, liked: Boolean, count: Int) {
        if (closed) return
        mutableState.update { it.copy(
            items = it.items.map { item ->
            if (item.rpid == rpid) item.copy(liked = liked, like = count)
            else item.copy(
                extras = item.extras.copy(
                    previewReplies = item.previewReplies.map { reply ->
                if (reply.rpid == rpid) reply.copy(liked = liked, like = count) else reply
            },
                ),
            )
        },
        ) }
    }

    /**
     * 本地刚发出的评论钉在第一条；下一次 [reload]/[reset] 会用 API 结果整体替换，
     * 因此刷新后自然回到服务端排序位置，不需要额外的去重状态。
     */
    fun prependLocal(item: CommentItem) {
        if (closed) return
        mutableState.update {
            it.copy(
                items = listOf(item) + it.items.filterNot { row -> row.rpid == item.rpid },
                total = it.total + 1,
            )
        }
    }

    /**
     * 主评论区里回复别人：先塞进对应根评论的折叠预览并让条数 +1。
     * 找不到根评论（已翻页出去）时返回 false，调用方退回“重载 + 定位”。
     */
    fun addLocalPreviewReply(rootRpid: Long, reply: CommentItem): Boolean {
        if (closed) return false
        var found = false
        mutableState.update { state ->
            val items = state.items.map { item ->
                if (item.rpid != rootRpid) item else {
                    found = true
                    item.copy(
                        replyCount = item.replyCount + 1,
                        extras = item.extras.copy(
                            previewReplies = mergePreviewReplies(item.previewReplies, listOf(reply)),
                        ),
                    )
                }
            }
            if (found) state.copy(items = items) else state
        }
        return found
    }

    fun remove(rpid: Long) { if (closed) return; mutableState.update { it.copy(items = it.items.filterNot { item -> item.rpid == rpid }, total = (it.total - 1).coerceAtLeast(0)) } }
}
