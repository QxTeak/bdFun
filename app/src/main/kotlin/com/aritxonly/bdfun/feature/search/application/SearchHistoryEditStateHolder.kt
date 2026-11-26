package com.aritxonly.bdfun.feature.search.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 搜索记录编辑态。长按某条记录进入，删掉的词先存在草稿里，
 * 点「保存」才落盘，点「取消」或离开搜索页直接丢弃草稿。
 */
internal data class SearchHistoryEditState(
    val editing: Boolean = false,
    /** 编辑期间可见的记录（起点是进入编辑时的快照，删一条少一条）。 */
    val visible: List<String> = emptyList(),
    /** 编辑期间被删掉的词，保存时一次提交。 */
    val removed: List<String> = emptyList(),
)

/**
 * 纯状态机，不碰持久化：删除动作只改内存，落盘由调用方在 [commit] 时负责。
 */
internal class SearchHistoryEditStateHolder {
    private val mutable = MutableStateFlow(SearchHistoryEditState())
    val state: StateFlow<SearchHistoryEditState> = mutable.asStateFlow()

    /** 长按进入编辑；已经在编辑时再长按一次不重置草稿（避免中途误触丢删除进度）。 */
    fun begin(history: List<String>) {
        if (mutable.value.editing) return
        mutable.value = SearchHistoryEditState(editing = true, visible = history, removed = emptyList())
    }

    /** 点叉号：只从草稿里拿掉，不落盘。 */
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