package com.aritxonly.bdfun.di

import com.aritxonly.bdfun.core.application.SessionIdentity
import com.aritxonly.bdfun.core.application.SessionResourceSlot
import com.aritxonly.bdfun.feature.feed.application.FeedStateHolder
import com.aritxonly.bdfun.feature.feed.application.FeedUiState
import com.aritxonly.bdfun.feature.feed.presentation.FeedState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 动态页各分类的状态槽。
 *
 * 每个分类独立列表/滚动/分页；UP 头像与直播 portal 只在「全部」分类加载，
 * 其它分类订阅同一份 StateFlow，避免每次切 Tab 重复请求。
 * 放在独立文件里，AppContainer 只保留接线。
 */
internal class DynamicFeedSlots(
    private val repository: com.aritxonly.bdfun.core.domain.FeedPageRepository,
    private val identity: () -> SessionIdentity,
    private val canLoadPortal: () -> Boolean,
    private val scope: CoroutineScope,
) {
    private data class Slot(val job: Job, val view: FeedState)

    private val slots = mutableMapOf<String, SessionResourceSlot<Slot>>()

    fun stateFor(type: String): FeedState {
        val key = type.takeIf { it.isNotBlank() } ?: "all"
        synchronized(slots) {
            slots[key]?.let { return it.get().view }
            val allView = if (key == "all") null else create("all", null).get().view
            val slot = create(key, allView?.sourceState)
            if (key != "all" && allView != null && canLoadPortal()) {
                scope.launch { allView.ensurePortalLoaded(true) }
            }
            return slot.get().view
        }
    }

    private fun create(key: String, portalSource: StateFlow<FeedUiState>?): SessionResourceSlot<Slot> {
        val slot = SessionResourceSlot(identity, {
            val job = SupervisorJob(scope.coroutineContext[Job])
            val child = CoroutineScope(scope.coroutineContext + job)
            Slot(
                job,
                FeedState(
                    FeedStateHolder(repository, key, loadPortal = key == "all"),
                    child,
                    portalSource,
                ),
            )
        }, { it.view.close(); it.job.cancel() })
        slots[key] = slot
        return slot
    }

    fun clearStale() {
        synchronized(slots) {
            slots.values.toList().forEach { it.clearStale() }
            // 非「全部」分类持有上一账号的 portal 流，账号切换后必须重建。
            slots.keys.filter { it != "all" }.forEach { slots.remove(it) }
        }
    }

    fun close() {
        synchronized(slots) { slots.values.toList() }.forEach { it.close() }
    }
}
