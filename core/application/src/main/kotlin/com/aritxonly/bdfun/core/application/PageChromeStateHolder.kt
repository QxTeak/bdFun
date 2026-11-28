package com.aritxonly.bdfun.core.application

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PageChromeState(
    val scrollTopRequest: Int = 0,
    val batchRequest: Int = 0,
    val searchRequest: Int = 0,
    val batchVisible: Boolean = false,
    val batchCount: Int = 0,
    val batchCanTransfer: Boolean = false,
    val batchActionRequest: Int = 0,
    val batchActionKey: String = "",
    val readAllRequest: Int = 0,
    val page: String? = null,
    val scrolled: Boolean = false,
)

/** 顶栏事件与页面浮层由页面壳持有，多个 Activity 不再相互覆盖。 */
class PageChromeStateHolder {
    private val mutableState = MutableStateFlow(PageChromeState())
    val state = mutableState.asStateFlow()
    fun update(transform: (PageChromeState) -> PageChromeState) { mutableState.update(transform) }
}
