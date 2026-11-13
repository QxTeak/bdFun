package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.navigation.Route
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal sealed interface MessageDestination {
    data class Feed(val kind: MessageFeedKind, val tab: Int = 0) : MessageDestination
    data class Page(val route: Route) : MessageDestination
}

internal data class MessageCenterEntry(val id: Int, val destination: MessageDestination)

/** 布局变化只改变展示位置，不创建第二套导航栈。 */
internal class MessageCenterStateHolder(restored: List<MessageDestination> = emptyList()) {
    private var nextId = restored.size
    private val mutable = MutableStateFlow(restored.mapIndexed { i, page -> MessageCenterEntry(i, page) })
    val entries = mutable.asStateFlow()

    fun open(destination: MessageDestination, fromMaster: Boolean = false) {
        val current = mutable.value
        if (current.lastOrNull()?.destination == destination) return
        mutable.value = (if (fromMaster) emptyList() else current) + MessageCenterEntry(nextId++, destination)
    }

    fun back(): Boolean {
        if (mutable.value.isEmpty()) return false
        mutable.value = mutable.value.dropLast(1)
        return true
    }
}

internal fun usesMessageSplitPane(widthDp: Float): Boolean = widthDp >= 840f
