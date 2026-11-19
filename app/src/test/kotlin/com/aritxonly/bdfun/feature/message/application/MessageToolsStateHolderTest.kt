package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class MessageToolsStateHolderTest {
    private class Fake : MessageManagementRepository {
        var gate: CompletableDeferred<Unit>? = null
        var people = listOf(MessageLikePerson(1, "甲", "", 0))
        var requests = mutableListOf<Pair<Int, Long>>()
        var failure = false
        var writes = 0
        var chat = ChatPreferences("", "", false, false, null, false)
        var list = MessageKeywordList(listOf("已有"), 2, 3, "已达上限")
        override suspend fun likes(id: String, page: Int, lastMid: Long): Outcome<MessageLikeDetail> {
            requests += page to lastMid; gate?.await()
            return if (failure) Outcome.Failure(AppError.Network("offline")) else Outcome.Success(MessageLikeDetail("原内容", people))
        }
        override suspend fun settings(type: Int) = Outcome.Success(ImSettingsPage("设置", listOf(ImSetting(1, "提醒", enabled = true))))
        override suspend fun setSetting(setting: ImSetting, enabled: Boolean?, choice: Int?): Outcome<Unit> {
            writes++; return if (failure) Outcome.Failure(AppError.Network("offline")) else Outcome.Success(Unit)
        }
        override suspend fun keywords() = Outcome.Success(list)
        override suspend fun addKeyword(keyword: String): Outcome<Unit> {
            writes++; gate?.await(); if (failure) return Outcome.Failure(AppError.Network("offline"))
            list = list.copy(keywords = list.keywords + keyword); return Outcome.Success(Unit)
        }
        override suspend fun deleteKeyword(keyword: String): Outcome<Unit> {
            list = list.copy(keywords = list.keywords - keyword); return Outcome.Success(Unit)
        }
        override suspend fun contacts(group: Int, query: String, page: Int) = Outcome.Success(MessageContactsPage(people, true))
        override suspend fun chatPreferences(mid: Long) = Outcome.Success(chat)
        override suspend fun setChatPreference(mid: Long, action: ChatPreferenceAction, enabled: Boolean) = Outcome.Success(Unit)
    }
    @Test fun likesAdvanceByPageAndLastMidAndStopOnRepeatedPeople() = runBlocking {
        val source = Fake(); val holder = MessageToolsStateHolder(source, MessageTool.Likes("id", 10))
        holder.load(); holder.load(true)
        assertEquals(listOf(1 to 0L, 2 to 1L), source.requests)
        assertFalse(holder.state.value.more)
        assertEquals(1, holder.state.value.people.size)
    }
    @Test fun failedPaginationRetriesSamePageAndRetainsExistingRows() = runBlocking {
        val source = Fake(); val holder = MessageToolsStateHolder(source, MessageTool.Likes("id", 3))
        holder.load(); source.failure = true; holder.load(true)
        assertEquals(1, holder.state.value.people.size); assertTrue(holder.state.value.retryMore)
        source.failure = false; source.people = listOf(MessageLikePerson(2, "乙", "", 0)); holder.load(true)
        assertEquals(listOf(1, 2, 2), source.requests.map { it.first })
        assertEquals(listOf(1L, 2L), holder.state.value.people.map { it.mid })
    }
    @Test fun closeAndAccountChangeDiscardLateLoads() = runBlocking {
        var owner = true
        val source = Fake().apply { gate = CompletableDeferred() }
        val holder = MessageToolsStateHolder(source, MessageTool.Likes("id", 2), { owner })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.load() }
        owner = false; source.gate!!.complete(Unit); job.join()
        assertTrue(holder.state.value.people.isEmpty())
        holder.close(); holder.load(); assertEquals(1, source.requests.size)
    }
    @Test fun cancelledKeywordMutationUnlocksAndKeepsDraft() = runBlocking {
        val source = Fake(); val holder = MessageToolsStateHolder(source, MessageTool.Keywords)
        holder.load(); holder.draft("新词"); source.gate = CompletableDeferred()
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.addKeyword() }
        job.cancelAndJoin()
        assertFalse(holder.state.value.busy); assertEquals("新词", holder.state.value.draft)
    }
    @Test fun keywordLimitsAreEnforcedBeforeNetworkAndFailureKeepsDraft() = runBlocking {
        val source = Fake(); val holder = MessageToolsStateHolder(source, MessageTool.Keywords)
        holder.load(); holder.draft("已有"); assertNotNull(holder.addKeyword())
        holder.draft("四个字符"); assertNotNull(holder.addKeyword()); assertEquals(0, source.writes)
        holder.draft("新词"); source.failure = true; assertEquals("offline", holder.addKeyword())
        assertEquals("新词", holder.state.value.draft); assertFalse(holder.state.value.busy)
    }
    @Test fun pinReadFailureKeepsOtherSettingsAndRetryUpdatesPin() = runBlocking {
        val source = Fake().apply {
            chat = chat.copy(pinned = null, muted = true, pinError = "置顶设置加载失败：1217 (1217)")
        }
        val holder = MessageToolsStateHolder(source, MessageTool.Chat(123))
        holder.load()
        val failed = holder.state.value.chat!!
        assertNull(failed.pinned)
        assertTrue(failed.muted)
        assertTrue(failed.pinError!!.contains("1217"))
        assertNull(holder.state.value.error)

        source.chat = source.chat.copy(pinned = true, pinError = null)
        holder.load()
        assertEquals(true, holder.state.value.chat?.pinned)
        assertNull(holder.state.value.chat?.pinError)
    }

    @Test fun failedSettingWriteKeepsServerSnapshotAndUnlocks() = runBlocking {
        val source = Fake(); val holder = MessageToolsStateHolder(source, MessageTool.Settings(19))
        holder.load(); source.failure = true
        assertEquals("offline", holder.setting(holder.state.value.settings.single(), false))
        assertTrue(holder.state.value.settings.single().enabled!!); assertFalse(holder.state.value.busy)
    }
}
