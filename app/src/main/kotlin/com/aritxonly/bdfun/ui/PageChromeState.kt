package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.application.PageChromeState
import com.aritxonly.bdfun.core.application.PageChromeStateHolder
import kotlinx.coroutines.*

internal val LocalPageChromeState = staticCompositionLocalOf<PageChromePresentationState> {
    error("列表与顶栏必须位于页面壳内")
}

@Composable
internal fun PageChromeScope(content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val state = remember { PageChromePresentationState(scope) }
    CompositionLocalProvider(LocalPageChromeState provides state, content = content)
}

internal class PageChromePresentationState(scope: CoroutineScope) {
    private val holder = PageChromeStateHolder()
    private var snapshot by mutableStateOf(holder.state.value)
    init { scope.launch(start = CoroutineStart.UNDISPATCHED) { holder.state.collect { snapshot = holder.state.value } } }
    private fun update(transform: (PageChromeState) -> PageChromeState) {
        holder.update(transform); snapshot = holder.state.value
    }
    var action by mutableStateOf<(() -> Unit)?>(null)
    var scrollTopRequest: Int
        get() = snapshot.scrollTopRequest
        set(value) = update { it.copy(scrollTopRequest = value) }
    var batchRequest: Int
        get() = snapshot.batchRequest
        set(value) = update { it.copy(batchRequest = value) }
    var searchRequest: Int
        get() = snapshot.searchRequest
        set(value) = update { it.copy(searchRequest = value) }
    var batchVisible: Boolean
        get() = snapshot.batchVisible
        set(value) = update { it.copy(batchVisible = value) }
    var batchCount: Int
        get() = snapshot.batchCount
        set(value) = update { it.copy(batchCount = value) }
    var batchCanTransfer: Boolean
        get() = snapshot.batchCanTransfer
        set(value) = update { it.copy(batchCanTransfer = value) }
    var batchActionRequest: Int
        get() = snapshot.batchActionRequest
        set(value) = update { it.copy(batchActionRequest = value) }
    var batchActionKey: String
        get() = snapshot.batchActionKey
        set(value) = update { it.copy(batchActionKey = value) }
    var readAllRequest: Int
        get() = snapshot.readAllRequest
        set(value) = update { it.copy(readAllRequest = value) }
    var moreAnchor by mutableStateOf(Rect.Zero)
    var moreRequest: Int
        get() = snapshot.moreRequest
        set(value) = update { it.copy(moreRequest = value) }
    var page: String?
        get() = snapshot.page
        set(value) = update { it.copy(page = value) }
    var scrolled: Boolean
        get() = snapshot.scrolled
        set(value) = update { it.copy(scrolled = value) }
    fun requestScrollTop() { scrollTopRequest++ }
    fun requestBatch() { batchRequest++ }
    fun requestSearch() { searchRequest++ }
    fun requestMore() { moreRequest++ }
    fun requestReadAll() { readAllRequest++ }
    fun sendBatchAction(key: String) = update { it.copy(batchActionKey = key, batchActionRequest = it.batchActionRequest + 1) }
    fun clearBatch() = update { it.copy(batchVisible = false, batchCount = 0, batchCanTransfer = false, batchActionKey = "") }
}
