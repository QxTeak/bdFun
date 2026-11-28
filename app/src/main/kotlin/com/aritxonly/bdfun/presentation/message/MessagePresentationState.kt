package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.message.MessageItem
import com.aritxonly.bdfun.core.model.message.MessageSession
import kotlinx.coroutines.*

internal class MessageSessionsPresentationStateHolder(readAll: Int) {
    var tabIndex by mutableIntStateOf(0)
    var consumedReadAll by mutableIntStateOf(readAll)
}

internal class MessageSessionMenuStateHolder {
    var selected by mutableStateOf<MessageSession?>(null)
    var confirmDelete by mutableStateOf(false)
    var sessionMenuVisible by mutableStateOf(false)
    var sessionAnchor by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    var confirmRead by mutableStateOf(false)
    var confirmClear by mutableStateOf(false)

    fun openSession(session: MessageSession, anchor: androidx.compose.ui.geometry.Rect) {
        selected = session
        sessionAnchor = anchor
        confirmDelete = false
        sessionMenuVisible = true
    }
}

internal class MessageFeedPresentationStateHolder {
    var tab by mutableIntStateOf(0)
}

internal class MessageFeedMenuStateHolder {
    var selected by mutableStateOf<Pair<Int, MessageItem>?>(null)
    var anchor by mutableStateOf(androidx.compose.ui.geometry.Rect.Zero)
    var showMenu by mutableStateOf(false)
    var confirmDelete by mutableStateOf(false)

    fun open(page: Int, item: MessageItem, bounds: androidx.compose.ui.geometry.Rect) {
        selected = page to item
        anchor = bounds
        showMenu = true
        confirmDelete = false
    }
}

internal data class MessageScreenEvents(
    val onNeedLogin: () -> Unit,
    val onOpenReplies: () -> Unit,
    val onOpenLikes: () -> Unit,
    val onOpenFans: () -> Unit,
)

@Composable
internal fun rememberMessageTaskScope(owner: Any, onClose: () -> Unit): CoroutineScope {
    val parent = rememberCoroutineScope()
    val scope = remember(owner) { CoroutineScope(parent.coroutineContext + SupervisorJob(parent.coroutineContext[Job])) }
    DisposableEffect(owner) {
        onDispose { scope.cancel(); onClose() }
    }
    return scope
}

internal fun messageFeedTabs(kind: MessageFeedKind) = when (kind) {
    MessageFeedKind.Reply -> listOf("回复我的", "@我")
    MessageFeedKind.Like -> emptyList()
    else -> emptyList()
}

internal fun messageFeedTitle(kind: MessageFeedKind) = when (kind) {
    MessageFeedKind.Reply -> "回复与@"
    MessageFeedKind.Like -> "收到喜欢"
    MessageFeedKind.Fans -> "新增粉丝"
    MessageFeedKind.System -> "系统通知"
}
