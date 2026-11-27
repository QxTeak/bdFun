package com.aritxonly.bdfun.feature.search.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 搜索记录编辑态。长按进入后每条记录右上角出现勾选框，
 * 勾选只是内存里的选择，点「删除」才一次性落盘；点「取消」直接退出。
 */
internal data class SearchHistoryEditState(
    val editing: Boolean = false,
    /** 编辑期间可见的记录；进入编辑时取快照，删除后立即消失。 */
    val visible: List<String> = emptyList(),
    /** 当前被勾选的词。 */
    val selected: Set<String> = emptySet(),
    /** 已提交删除的词，保留给旧调用方/测试读取。 */
    val removed: List<String> = emptyList(),
)

/**
 * 纯状态机，不碰持久化：删除动作只改内存，落盘由调用方在 [commit] 时负责。
 */
internal class SearchHistoryEditStateHolder {
    private val mutable = MutableStateFlow(SearchHistoryEditState())
    val state: StateFlow<SearchHistoryEditState> = mutable.asStateFlow()

    /** 长按进入编辑；已经在编辑时再长按一次不重置选择（避免中途误触丢进度）。 */
    fun begin(history: List<String>) {
        if (mutable.value.editing) return
        mutable.value = SearchHistoryEditState(editing = true, visible = history)
    }

    /** 点一下记录：切换勾选，不立即删除。 */
    fun toggle(word: String) {
        mutable.update { current ->
            if (!current.editing || word !in current.visible) return@update current
            current.copy(
                selected = if (word in current.selected) current.selected - word else current.selected + word,
            )
        }
    }

    /** 点「删除」：只从草稿里拿掉已勾选项，不落盘。 */
    fun deleteSelected() {
        mutable.update { current ->
            if (!current.editing || current.selected.isEmpty()) return@update current
            current.copy(
                visible = current.visible - current.selected,
                removed = current.removed + current.selected,
                selected = emptySet(),
            )
        }
    }

    /** 兼容旧入口：删除单条（不进入勾选流程时仍可用）。 */
    fun remove(word: String) {
        mutable.update { current ->
            if (!current.editing || word !in current.visible) return@update current
            current.copy(visible = current.visible - word, removed = current.removed + word)
        }
    }

    /** 取消：草稿整个丢掉，历史保持原样。 */
    fun cancel() { mutable.value = SearchHistoryEditState() }

    /**
     * 保存：把草稿里的删除交给 [apply]，返回被删掉的词，调用方据此刷新界面。
     * 落盘失败也要退出编辑态——停在编辑里会让用户以为改动没生效。
     */
    fun commit(apply: (Collection<String>) -> Unit): List<String> {
        val current = mutable.value
        if (!current.editing) return emptyList()
        mutable.value = SearchHistoryEditState()
        if (current.removed.isNotEmpty()) apply(current.removed)
        return current.removed
    }
}
