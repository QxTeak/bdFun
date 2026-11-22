package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder
import com.aritxonly.bdfun.feature.message.application.MessageSessionsStateHolder

/**
 * 消息页只在进入时自动加载一次；从回复页/收到的赞页返回不再重载，
 * 需要新数据时由用户下拉刷新。首次加载不显示小电视。
 */
@Composable
internal fun MessageSessionsEffects(
    controller: MessageSessionsStateHolder,
    loggedIn: Boolean,
    ui: MessageSessionsPresentationStateHolder,
    menu: MessageSessionMenuStateHolder,
    scope: kotlinx.coroutines.CoroutineScope,
) {
    val chrome = LocalPageChromeState.current
    LaunchedEffect(controller, loggedIn) {
        if (!loggedIn) {
            controller.reset()
        } else if (controller.state.value.sessions.isEmpty()) {
            controller.load()
        }
    }
    LaunchedEffect(controller, chrome.readAllRequest) {
        if (ui.consumedReadAll == chrome.readAllRequest) return@LaunchedEffect
        ui.consumedReadAll = chrome.readAllRequest
        if (!loggedIn) return@LaunchedEffect
        menu.confirmRead = true
    }
    LaunchedEffect(chrome.moreRequest) {
        if (ui.consumedMore != chrome.moreRequest) {
            ui.consumedMore = chrome.moreRequest
            menu.showMore = true
        }
    }
}

@Composable
internal fun MessageFeedEffects(controller: MessageFeedStateHolder, ui: MessageFeedPresentationStateHolder, scope: kotlinx.coroutines.CoroutineScope) {
    // 切换 tab 只为还没加载过的 tab 拉一次；返回本页不再自动重载。
    LaunchedEffect(controller, ui.tab) {
        val key = if (ui.tab == 1) 0 else ui.tab
        if (!controller.state.value.itemsByTab.containsKey(key)) controller.loadPage(key)
    }
}
