package com.aritxonly.bdfun.ui

import android.content.Context
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.domain.SessionAction
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.bdfun.feature.message.application.MessageFeedStateHolder
import com.aritxonly.bdfun.feature.message.application.MessageSessionsStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class MessageSessionActions(
    private val controller: MessageSessionsStateHolder,
    private val scope: CoroutineScope,
    private val context: Context,
    private val loggedIn: Boolean,
    private val menu: MessageSessionMenuStateHolder,
) {
    fun load() { if (loggedIn) scope.launch { controller.load() } }
    fun loadFooter() { if (loggedIn) scope.launch { controller.loadFooter() } }
    fun readAll() { scope.launch { controller.clearUnread()?.let { context.showToast(it) }; menu.confirmRead = false } }
    fun clearList() { scope.launch { controller.clearList()?.let { context.showToast(it) }; menu.confirmClear = false } }
    fun mutate(action: SessionAction) {
        val target = menu.selected ?: return
        scope.launch {
            when (val result = controller.mutate(target, action)) {
                is Outcome.Success -> { menu.selected = null; menu.confirmDelete = false }
                is Outcome.Failure -> context.showToast(result.error.messageOr("操作失败"))
                null -> Unit
            }
        }
    }
}

internal class MessageFeedActions(
    private val controller: MessageFeedStateHolder,
    private val scope: CoroutineScope,
    private val context: Context,
    private val menu: MessageFeedMenuStateHolder,
    private val navigate: (com.aritxonly.bdfun.core.navigation.Route) -> Unit,
) {
    fun load(tab: Int) { scope.launch { controller.loadPage(tab) } }
    fun loadFooter(tab: Int) { scope.launch { controller.loadFooter(tab) } }
    fun open(item: MessageItem) {
        if (controller.kind == MessageFeedKind.Like && item.likeCount > 1) {
            navigate(com.aritxonly.bdfun.core.navigation.Route.MessageTools("likes", item.id, count = item.likeCount, navigation = item.navigation))
        } else openMessageItem(item, controller.kind, controller, scope, context)
    }
    fun toggleFollow(item: MessageItem) {
        scope.launch { controller.toggleFollow(item)?.let { context.showToast(it) } }
    }
    fun mutate(delete: Boolean) {
        val (page, item) = menu.selected ?: return
        scope.launch {
            when (val result = controller.mutate(page, item, delete)) {
                is Outcome.Success -> { menu.selected = null; menu.confirmDelete = false; menu.confirmMute = false }
                is Outcome.Failure -> context.showToast(result.error.messageOr("操作失败"))
                null -> Unit
            }
        }
    }
}
