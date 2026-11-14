package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.navigation.Route
import org.junit.Assert.*
import org.junit.Test

class MessageCenterStateHolderTest {
    @Test fun combinedReplyEntryIncludesBothUnreadCounters() {
        assertEquals(5, mapOf("reply" to 3, "at" to 2, "like" to 99).replyAndMentionUnread())
        assertEquals(2, mapOf("at" to 2).replyAndMentionUnread())
        assertEquals(3, mapOf("reply" to 3).replyAndMentionUnread())
        assertEquals(0, emptyMap<String, Int>().replyAndMentionUnread())
    }

    @Test fun followingTabFiltersWithoutChangingPaginationSource() {
        val followed = com.aritxonly.bdfun.core.model.message.MessageSession(
            "1_1", 1, 1, "关注的人", "", "", 0, 0, false,
            preferences = com.aritxonly.bdfun.core.model.message.MessageSessionPreferences(followed = true),
        )
        val other = followed.copy(id = "1_2", talkerId = 2,
            preferences = followed.preferences.copy(followed = false))
        val state = MessageSessionsUiState(sessions = listOf(followed, other), inboxCursor = "next")
        assertEquals(listOf(followed), state.visibleSessions(true))
        assertEquals(listOf(followed, other), state.visibleSessions(false))
        assertEquals("next", state.inboxCursor)
    }

    @Test fun coverScreenStaysSinglePane() {
        assertFalse(usesMessageSplitPane(390f))
        assertFalse(usesMessageSplitPane(480f))
        assertFalse(usesMessageSplitPane(839f))
        assertTrue(usesMessageSplitPane(840f))
        assertTrue(usesMessageSplitPane(1024f))
    }

    @Test fun settingsBackReturnsToSameChatEntryAndResizeDoesNotChangeStack() {
        val holder = MessageCenterStateHolder()
        val chat = MessageDestination.Page(Route.PrivateChat(42))
        holder.open(chat, fromMaster = true)
        val entry = holder.entries.value.single()
        holder.open(MessageDestination.Page(Route.MessageTools("chat", "42")))
        assertTrue(holder.back())
        assertEquals(entry, holder.entries.value.single())
        usesMessageSplitPane(1024f)
        usesMessageSplitPane(390f)
        assertEquals(entry, holder.entries.value.single())
        assertTrue(holder.back())
        assertFalse(holder.back())
    }

    @Test fun masterSelectionReplacesDetailStackAndRepeatedTapDoesNotDuplicate() {
        val holder = MessageCenterStateHolder()
        holder.open(MessageDestination.Page(Route.PrivateChat(1)), true)
        holder.open(MessageDestination.Page(Route.MessageTools("chat", "1")))
        val likes = MessageDestination.Feed(MessageFeedKind.Like)
        holder.open(likes, true)
        holder.open(likes, true)
        assertEquals(listOf(likes), holder.entries.value.map { it.destination })
    }

    @Test fun restoredStackKeepsMentionTabAndUsesFreshEntryIds() {
        val mention = MessageDestination.Feed(MessageFeedKind.Reply, 2)
        val holder = MessageCenterStateHolder(listOf(mention))
        holder.open(MessageDestination.Page(Route.MessageTools("settings")))
        assertEquals(2, holder.entries.value.map { it.id }.distinct().size)
        holder.back()
        assertEquals(mention, holder.entries.value.single().destination)
    }
}
