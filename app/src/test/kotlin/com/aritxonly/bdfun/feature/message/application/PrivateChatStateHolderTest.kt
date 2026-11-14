package com.aritxonly.bdfun.feature.message.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.message.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PrivateChatStateHolderTest {
    @Test fun loadFailurePreservesServerReasonAndSuccessfulRetryClearsIt() = runBlocking {
        val source = Fake()
        source.page = { failure("测试服务端错误 (1217)") }
        val state = holder(source) { }
        state.load()
        assertEquals("测试服务端错误 (1217)", state.state.value.loadErrorText)
        source.page = { Outcome.Success(ChatMessagePage(emptyList(), null)) }
        state.load()
        assertNull(state.state.value.loadErrorText)
        assertFalse(state.state.value.loadError)
    }

    private val session = MessageSession("1_2", 2, 1, "对方", "", "", 0, 0, false)
    private fun message(id: Long, text: String = "$id", seq: Long = id) = ChatMessage(id, 2, 1, text, sequence = seq)
    private fun failure(text: String = "offline") = Outcome.Failure(AppError.Unknown(text))
    private class Fake : PrivateChatRepository {
        val calls = mutableListOf<String>()
        var page: suspend (Long?) -> Outcome<ChatMessagePage> = { Outcome.Success(ChatMessagePage(emptyList(), null)) }
        var upload: suspend (String) -> Outcome<ChatUploadedImage> = { Outcome.Success(ChatUploadedImage(it, 12, 34, 0.0)) }
        var text: suspend (String) -> Outcome<Boolean> = { Outcome.Success(true) }
        var image: suspend () -> Outcome<Boolean> = { Outcome.Success(true) }
        var acknowledge: Outcome<Unit> = Outcome.Success(Unit)
        override suspend fun page(session: MessageSession, before: Long?): Outcome<ChatMessagePage> {
            calls += "page:$before"; return page(before)
        }
        override suspend fun acknowledge(session: MessageSession, sequence: Long): Outcome<Unit> {
            calls += "ack:$sequence"; return acknowledge
        }
        override suspend fun myAvatar(): Outcome<String> { calls += "avatar"; return Outcome.Success("avatar") }
        override suspend fun uploadImage(reference: String): Outcome<ChatUploadedImage> {
            calls += "upload:$reference"; return upload(reference)
        }
        override suspend fun sendImage(session: MessageSession, image: ChatUploadedImage): Outcome<Boolean> {
            calls += "image:${image.url}"; return this.image()
        }
        override suspend fun sendText(session: MessageSession, text: String): Outcome<Boolean> {
            calls += "text:$text"; return this.text(text)
        }
    }
    private fun holder(source: Fake, wait: suspend (Long) -> Unit = {}) = PrivateChatStateHolder(source, session, wait)

    @Test fun identityRevisionRejectsUploadBeforeAnySendOrDisposal() = runBlocking {
        var revision = 1
        val source = Fake()
        val gate = CompletableDeferred<Outcome<ChatUploadedImage>>()
        source.upload = { gate.await() }
        val holder = PrivateChatStateHolder(source, session, isCurrentOwner = { revision == 1 })
        holder.setImage("local")
        val job = async(start = CoroutineStart.UNDISPATCHED) { holder.send({ true }, {}) }
        revision = 3
        gate.complete(Outcome.Success(ChatUploadedImage("uploaded", 12, 34, 0.0)))
        try { job.await(); fail("旧账号上传后不应继续发送") } catch (_: CancellationException) { }
        assertEquals(listOf("upload:local"), source.calls)
        assertNull(holder.send({ true }, {}))
    }

    @Test fun initialLoadRetriesThreeTimesThenAcknowledgesMaximumSequence() = runBlocking {
        val source = Fake(); var attempt = 0; val waits = mutableListOf<Long>()
        source.page = { if (++attempt < 3) failure() else Outcome.Success(ChatMessagePage(
            listOf(message(7, seq = 30), message(2, seq = 40)), 9)) }
        val state = holder(source) { waits += it }; state.load()
        assertEquals(listOf(400L, 400L), waits)
        assertEquals(listOf("page:null", "page:null", "page:null", "ack:40", "avatar"), source.calls)
        assertEquals(9L, state.state.value.historyCursor)
        assertEquals("avatar", state.state.value.myAvatar)
        assertFalse(state.state.value.loading); assertFalse(state.state.value.loadError)
    }

    @Test fun failedReloadKeepsHistoryButShowsLoadErrorAndStopsPolling() = runBlocking {
        val source = Fake(); source.page = { Outcome.Success(ChatMessagePage(listOf(message(1)), 10)) }
        val state = holder(source); state.load(); source.calls.clear(); source.page = { failure() }
        state.load()
        assertEquals(3, source.calls.count { it == "page:null" })
        assertTrue(state.state.value.loadError); assertFalse(state.state.value.loading)
        assertEquals(listOf(message(1)), state.state.value.messages)
        source.calls.clear(); state.pollLatest { true }; assertTrue(source.calls.isEmpty())
    }

    @Test fun cancellingInitialRequestDoesNotRetryOrTurnIntoLoadError() = runBlocking {
        val source = Fake(); val gate = CompletableDeferred<Outcome<ChatMessagePage>>()
        source.page = { gate.await() }; val state = holder(source)
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.load() }
        job.cancelAndJoin()
        assertEquals(listOf("page:null"), source.calls)
        assertFalse(state.state.value.loading); assertFalse(state.state.value.loadError)
    }

    @Test fun historyUsesOlderCopyForDuplicateAndLatestRefreshUsesNewerCopy() = runBlocking {
        val source = Fake(); source.page = { Outcome.Success(ChatMessagePage(listOf(message(2)), 20)) }
        val state = holder(source); state.load()
        source.page = { before ->
            if (before != null) Outcome.Success(ChatMessagePage(listOf(message(1), message(2, "older")), 10))
            else Outcome.Success(ChatMessagePage(listOf(message(2, "newer"), message(3)), 99))
        }
        state.loadHistory()
        assertEquals(listOf("1", "older"), state.state.value.messages.map { it.text })
        state.refreshLatest { false }
        assertEquals(listOf("1", "newer", "3"), state.state.value.messages.map { it.text })
        assertEquals(10L, state.state.value.historyCursor)
    }

    @Test fun historyBusyAndFailureKeepTheCursorForRetry() = runBlocking {
        val source = Fake(); source.page = { Outcome.Success(ChatMessagePage(emptyList(), 10)) }
        val state = holder(source); state.load(); source.calls.clear()
        val gate = CompletableDeferred<Outcome<ChatMessagePage>>(); source.page = { gate.await() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.loadHistory() }
        state.loadHistory(); assertEquals(listOf("page:10"), source.calls)
        gate.complete(failure("历史原文")); job.join()
        assertEquals("历史原文", state.state.value.historyError)
        assertEquals(10L, state.state.value.historyCursor); assertFalse(state.state.value.historyLoading)
    }

    @Test fun refreshSamplesReadReceiptGateAfterFetchingAndKeepsMergedMessagesOnAckFailure() = runBlocking {
        val source = Fake(); val state = holder(source); state.load(); source.calls.clear()
        val gate = CompletableDeferred<Outcome<ChatMessagePage>>(); source.page = { gate.await() }
        var atBottom = false
        val job = async(start = CoroutineStart.UNDISPATCHED) { state.refreshLatest { atBottom } }
        atBottom = true; source.acknowledge = failure("ack failed")
        gate.complete(Outcome.Success(ChatMessagePage(listOf(message(4, seq = 50)), null)))
        assertTrue(job.await() is Outcome.Failure)
        assertEquals(listOf("page:null", "ack:50"), source.calls)
        assertEquals(4L, state.state.value.messages.single().id)
    }

    @Test fun pollingDoesNotFetchBeforeInitialLoadOrDuringSend() = runBlocking {
        val source = Fake(); val state = holder(source)
        state.pollLatest { true }; assertTrue(source.calls.isEmpty())
        state.load(); source.calls.clear()
        val gate = CompletableDeferred<Outcome<Boolean>>(); source.text = { gate.await() }; state.setDraft("text")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.send({ true }, {}) }
        state.pollLatest { true }; assertEquals(listOf("text:text"), source.calls)
        job.cancelAndJoin(); assertFalse(state.state.value.composer.sending)
    }

    @Test fun emptyDraftDoesNotSendAndTextOnlyTrimsClearsAndClosesEmoji() = runBlocking {
        val source = Fake(); val state = holder(source)
        state.setDraft("   "); state.send({ true }, { error("不能滚动") }); assertTrue(source.calls.isEmpty())
        state.setDraft(" text "); state.toggleEmoji()
        assertNull(state.send({ false }, { source.calls += "scroll" }))
        assertEquals(listOf("text:text", "page:null", "scroll"), source.calls)
        assertEquals("", state.state.value.composer.draft)
        assertFalse(state.state.value.composer.showEmoji); assertFalse(state.state.value.composer.sending)
    }

    @Test fun editingDraftDuringSendKeepsTheNewTextAndBlocksDuplicateSend() = runBlocking {
        val source = Fake(); val gate = CompletableDeferred<Outcome<Boolean>>(); source.text = { gate.await() }
        val state = holder(source); state.setDraft("old")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.send({ false }, {}) }
        state.setDraft("new"); state.send({ false }, {})
        assertEquals(listOf("text:old"), source.calls)
        gate.complete(Outcome.Success(true)); job.join()
        assertEquals("new", state.state.value.composer.draft)
    }

    @Test fun successfulImageIsNotRepeatedWhenFollowingTextFails() = runBlocking {
        val source = Fake(); source.text = { Outcome.Success(false) }
        val state = holder(source); state.setDraft("words"); state.setImage("content://image"); state.toggleEmoji()
        assertEquals("文字发送失败，请重试", state.send({ false }, {}))
        assertNull(state.state.value.composer.pendingImage)
        assertEquals("words", state.state.value.composer.draft)
        assertTrue(state.state.value.composer.showEmoji)
        assertFalse(state.state.value.composer.uploading); assertFalse(state.state.value.composer.sending)
        source.text = { Outcome.Success(true) }; state.send({ false }, {})
        assertEquals(1, source.calls.count { it.startsWith("upload:") })
        assertEquals(1, source.calls.count { it.startsWith("image:") })
        assertEquals(2, source.calls.count { it == "text:words" })
    }

    @Test fun uploadOrImageSendFailureKeepsAttachmentAndDoesNotSendText() = runBlocking {
        for (uploadFails in listOf(true, false)) {
            val source = Fake()
            if (uploadFails) source.upload = { failure("") } else source.image = { Outcome.Success(false) }
            val state = holder(source); state.setDraft("words"); state.setImage("content://image")
            assertEquals(if (uploadFails) "" else "图片发送失败", state.send({ false }, {}))
            assertEquals("content://image", state.state.value.composer.pendingImage)
            assertTrue(source.calls.none { it.startsWith("text:") })
            assertFalse(state.state.value.composer.sending); assertFalse(state.state.value.composer.uploading)
        }
    }

    @Test fun refreshFailureAfterSendReturnsTheOriginalSentToast() = runBlocking {
        val source = Fake(); source.page = { failure() }; val state = holder(source); state.setDraft("text")
        assertEquals("已发送，消息列表稍后更新", state.send({ false }, { error("不应滚动") }))
        assertEquals("", state.state.value.composer.draft); assertFalse(state.state.value.composer.sending)
    }

    @Test fun scrollingFailureAfterSendKeepsSentStateAndReleasesBusy() = runBlocking {
        val source = Fake(); val state = holder(source); state.setDraft("text")
        assertEquals("已发送，消息列表稍后更新", state.send({ false }, { throw IllegalStateException() }))
        assertEquals("", state.state.value.composer.draft); assertFalse(state.state.value.composer.sending)
    }

    @Test fun cancellationRetainsUnsentImageAndReleasesBothFlags() = runBlocking {
        val source = Fake(); val gate = CompletableDeferred<Outcome<ChatUploadedImage>>(); source.upload = { gate.await() }
        val state = holder(source); state.setImage("content://image")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.send({ false }, {}) }
        assertTrue(state.state.value.composer.uploading)
        job.cancelAndJoin()
        assertEquals("content://image", state.state.value.composer.pendingImage)
        assertFalse(state.state.value.composer.uploading); assertFalse(state.state.value.composer.sending)
    }

    @Test fun closingScopePreventsOldUploadFromSendingAnything() = runBlocking {
        val source = Fake(); val gate = CompletableDeferred<Outcome<ChatUploadedImage>>(); source.upload = { gate.await() }
        val state = holder(source); state.setImage("content://image"); state.setDraft("text")
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.send({ false }, { error("旧页面不能滚动") }) }
        state.close(); gate.complete(Outcome.Success(ChatUploadedImage("url", 1, 1, 0.0))); job.join()
        assertTrue(job.isCancelled)
        assertEquals(listOf("upload:content://image"), source.calls)
        assertFalse(state.state.value.composer.uploading); assertFalse(state.state.value.composer.sending)
    }

    @Test fun cancelledHistoryRequestReleasesBusyAndKeepsItsCursor() = runBlocking {
        val source = Fake(); source.page = { Outcome.Success(ChatMessagePage(emptyList(), 10)) }
        val state = holder(source); state.load()
        val gate = CompletableDeferred<Outcome<ChatMessagePage>>(); source.page = { gate.await() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.loadHistory() }
        job.cancelAndJoin()
        assertFalse(state.state.value.historyLoading)
        assertNull(state.state.value.historyError); assertEquals(10L, state.state.value.historyCursor)
    }

    @Test fun initialAcknowledgementFailureDoesNotHideMessagesOrSkipOwnAvatar() = runBlocking {
        val source = Fake(); source.page = { Outcome.Success(ChatMessagePage(listOf(message(1)), null)) }
        source.acknowledge = failure("ack failure")
        val state = holder(source); state.load()
        assertEquals(listOf(message(1)), state.state.value.messages)
        assertEquals("avatar", state.state.value.myAvatar); assertFalse(state.state.value.loadError)
    }

    @Test fun closingDuringRetryWaitPreventsTheNextRequest() = runBlocking {
        val source = Fake(); source.page = { failure() }; val gate = CompletableDeferred<Unit>()
        val state = holder(source) { gate.await() }
        val job = launch(start = CoroutineStart.UNDISPATCHED) { state.load() }
        state.close(); gate.complete(Unit); job.join()
        assertTrue(job.isCancelled); assertEquals(listOf("page:null"), source.calls)
        assertFalse(state.state.value.loading)
    }
}
