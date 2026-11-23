package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.user.SearchUser
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class SearchStateHolderTest {
    private fun video(id: String) = VideoItem(id, 1, id, "", "", "", 1, 0, 0, 10, 0)
    private val user = SearchUser(1, "用户", "", "")
    private class Fake : SearchRepository {
        var hotCalls = 0
        val calls = mutableListOf<String>()
        val requests = mutableListOf<SearchRequest>()
        var suggest: suspend (String) -> Outcome<List<String>> = { Outcome.Success(listOf(it)) }
        var people: suspend (String) -> Outcome<List<SearchUser>> = { Outcome.Success(emptyList()) }
        /** 需要区分页码的用户分页测试用它；为空时沿用 [people]。 */
        var peoplePage: (suspend (String, Int) -> Outcome<List<SearchUser>>)? = null
        val userPages = mutableListOf<Int>()
        var videos: suspend (SearchRequest) -> Outcome<List<VideoItem>> = { Outcome.Success(emptyList()) }
        override suspend fun hotKeywords(): Outcome<List<SearchHotWord>> { hotCalls++; return Outcome.Success(listOf(SearchHotWord("热词"))) }
        override suspend fun discoveryKeywords(): Outcome<List<String>> = Outcome.Success(listOf("发现"))
        override suspend fun suggestions(query: String): Outcome<List<String>> { calls += "suggest:$query"; return suggest(query) }
        override suspend fun users(keyword: String, page: Int): Outcome<List<SearchUser>> {
            calls += "users:$keyword"; userPages += page
            return peoplePage?.invoke(keyword, page) ?: people(keyword)
        }
        override suspend fun videos(request: SearchRequest): Outcome<List<VideoItem>> {
            calls += "videos:${request.keyword}"; requests += request; return videos.invoke(request)
        }
        override suspend fun pgc(keyword: String, bangumi: Boolean, page: Int) =
            Outcome.Success(emptyList<com.aritxonly.bdfun.core.model.pgc.SearchPgcItem>())
    }

    @Test fun hotListLoadsOnlyWhenEnabledAndEmpty() = runBlocking {
        val source = Fake(); val holder = SearchStateHolder(source, wait = {})
        holder.loadHotKeywords(false)
        assertEquals(0, source.hotCalls)
        holder.loadHotKeywords(true); holder.loadHotKeywords(true)
        assertEquals(1, source.hotCalls)
        assertEquals(listOf(SearchHotWord("热词")), holder.state.value.hotKeywords)
    }

    @Test fun discoveryLoadsOnlyWhenEnabledAndEmptyAndStaysSeparateFromHot() = runBlocking {
        val source = Fake(); val holder = SearchStateHolder(source, wait = {})
        holder.loadDiscovery(false)
        assertTrue(holder.state.value.discoveryKeywords.isEmpty())
        holder.loadDiscovery(true); holder.loadDiscovery(true)
        assertEquals(listOf("发现"), holder.state.value.discoveryKeywords)
        // 两个板块各自独立：发现不会写进热搜，热搜也不会盖掉发现。
        assertTrue(holder.state.value.hotKeywords.isEmpty())
        holder.loadHotKeywords(true)
        assertEquals(listOf("发现"), holder.state.value.discoveryKeywords)
        assertEquals(listOf(SearchHotWord("热词")), holder.state.value.hotKeywords)
    }

    @Test fun suggestionsTrimDebounceAndClearOnBlankOrSubmitted() = runBlocking {
        val source = Fake(); val waits = mutableListOf<Long>()
        val holder = SearchStateHolder(source, wait = { waits += it })
        holder.suggest("  keyword  ", null)
        assertEquals(listOf("keyword"), holder.state.value.suggestions)
        assertEquals(listOf(180L), waits)
        holder.suggest("keyword", "keyword")
        assertTrue(holder.state.value.suggestions.isEmpty())
        holder.suggest("  ", null)
        assertEquals(listOf("suggest:keyword"), source.calls)
    }

    @Test fun staleSuggestionResponseCannotReplaceTheCurrentQuery() = runBlocking {
        val source = Fake(); val old = CompletableDeferred<Outcome<List<String>>>()
        source.suggest = { if (it == "old") old.await() else Outcome.Success(listOf(it)) }
        val holder = SearchStateHolder(source, wait = {})
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.suggest("old", null) }
        holder.suggest("new", null)
        old.complete(Outcome.Success(listOf("过期"))); job.join()
        assertEquals(listOf("new"), holder.state.value.suggestions)
    }

    @Test fun usersLoadBeforeVideosAndAreReusedForTheSameKeyword() = runBlocking {
        val source = Fake(); source.people = { Outcome.Success(listOf(user)) }
        val waits = mutableListOf<Long>(); val holder = SearchStateHolder(source, wait = { waits += it })
        holder.search("word", SearchFilterState())
        holder.search("word", SearchFilterState(order = 2, duration = 5))
        assertEquals(listOf("users:word", "videos:word", "videos:word"), source.calls)
        assertEquals(listOf(90L, 180L), waits)
        assertEquals(SearchRequest("word", "click", 5, null, null), source.requests.last())
        assertEquals(listOf(user), holder.state.value.users)
        assertFalse(holder.state.value.loading)
        assertTrue(holder.state.value.loadedOnce)
    }

    @Test fun failedUserSearchStillLoadsVideosAndRetriesUsersNextTime() = runBlocking {
        val source = Fake(); source.people = { Outcome.Failure(AppError.Network("用户失败")) }
        source.videos = { Outcome.Success(listOf(video(it.keyword))) }
        val holder = SearchStateHolder(source, wait = {})
        holder.search("word", SearchFilterState()); holder.search("word", SearchFilterState())
        assertEquals(listOf("users:word", "videos:word", "users:word", "videos:word"), source.calls)
        assertEquals("word", holder.state.value.results.single().bvid)
        assertNull(holder.state.value.error)
    }

    @Test fun videoFailureKeepsPreviousResultsAndRetainsEvenBlankErrorText() = runBlocking {
        val source = Fake(); source.videos = { Outcome.Success(listOf(video("old"))) }
        val holder = SearchStateHolder(source, wait = {})
        holder.search("word", SearchFilterState())
        source.videos = { Outcome.Failure(AppError.Unknown("")) }
        holder.search("new", SearchFilterState())
        assertEquals("old", holder.state.value.results.single().bvid)
        assertEquals("", holder.state.value.error)
        assertTrue(holder.state.value.loadedOnce)
        assertFalse(holder.state.value.loading)
    }

    @Test fun returningToLandingClearsResultsAndKeywordCacheButKeepsHotList() = runBlocking {
        val source = Fake(); source.people = { Outcome.Success(listOf(user)) }
        source.videos = { Outcome.Success(listOf(video("one"))) }
        val holder = SearchStateHolder(source, wait = {})
        holder.loadHotKeywords(true); holder.search("word", SearchFilterState())
        holder.search(null, SearchFilterState())
        assertTrue(holder.state.value.results.isEmpty())
        assertTrue(holder.state.value.users.isEmpty())
        assertFalse(holder.state.value.loadedOnce)
        assertFalse(holder.state.value.loading)
        assertEquals(listOf(SearchHotWord("热词")), holder.state.value.hotKeywords)
        holder.search("word", SearchFilterState())
        assertEquals(2, source.calls.count { it == "users:word" })
    }

    @Test fun cancellationReleasesLoadingWithoutAnEmptyOrErrorFlash() = runBlocking {
        val source = Fake(); val pending = CompletableDeferred<Outcome<List<SearchUser>>>()
        source.people = { pending.await() }; val holder = SearchStateHolder(source, wait = {})
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("word", SearchFilterState()) }
        assertTrue(holder.state.value.loading)
        job.cancelAndJoin()
        assertFalse(holder.state.value.loading)
        assertFalse(holder.state.value.loadedOnce)
        assertNull(holder.state.value.error)
        assertTrue(source.requests.isEmpty())
    }

    @Test fun staleVideoResponseCannotReplaceNewSearchOrFinishItsLoading() = runBlocking {
        val source = Fake(); val old = CompletableDeferred<Outcome<List<VideoItem>>>()
        val current = CompletableDeferred<Outcome<List<VideoItem>>>()
        source.videos = { if (it.keyword == "old") old.await() else current.await() }
        val holder = SearchStateHolder(source, wait = {})
        val oldJob = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("old", SearchFilterState()) }
        val currentJob = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("new", SearchFilterState()) }
        old.complete(Outcome.Success(listOf(video("old")))); oldJob.join()
        assertTrue(holder.state.value.loading)
        assertTrue(holder.state.value.results.isEmpty())
        current.complete(Outcome.Success(listOf(video("new")))); currentJob.join()
        assertEquals("new", holder.state.value.results.single().bvid)
        assertFalse(holder.state.value.loading)
    }

    @Test fun cancelledDebounceDoesNotSendARequest() = runBlocking {
        val source = Fake(); val gate = CompletableDeferred<Unit>()
        val holder = SearchStateHolder(source, wait = { gate.await() })
        val job = launch(start = CoroutineStart.UNDISPATCHED) { holder.search("word", SearchFilterState()) }
        job.cancelAndJoin()
        assertTrue(source.calls.isEmpty())
        assertFalse(holder.state.value.loading)
    }

    @Test fun publicationParametersKeepTheOriginalClockReadOrder() = runBlocking {
        val source = Fake(); val times = ArrayDeque(listOf(2_000_000_000_999L, 2_000_000_001_999L, 2_000_000_002_999L))
        val holder = SearchStateHolder(source, wait = {}, nowMillis = { times.removeFirst() })
        holder.search("word", SearchFilterState(published = 1))
        assertEquals(2_000_000_000L - 86400, source.requests.single().pubtimeBegin)
        assertEquals(2_000_000_002L, source.requests.single().pubtimeEnd)
        assertTrue(times.isEmpty())
    }

    @Test fun loadMoreAppendsTheNextPageAndStopsAtAShortPage() = runBlocking {
        val source = Fake()
        val firstPage = (1..SEARCH_PAGE_SIZE).map { video("a$it") }
        val secondPage = (1..3).map { video("b$it") }
        source.videos = { if (it.page == 1) Outcome.Success(firstPage) else Outcome.Success(secondPage) }
        val holder = SearchStateHolder(source, wait = {})
        holder.search("word", SearchFilterState())
        assertFalse(holder.state.value.end)
        holder.loadMore()
        assertEquals(2, source.requests[1].page)
        assertEquals(SEARCH_PAGE_SIZE + 3, holder.state.value.results.size)
        assertTrue(holder.state.value.end)
        assertFalse(holder.state.value.loadingMore)
    }

    @Test fun loadMoreDoesNothingBeforeASearchOrAfterTheEnd() = runBlocking {
        val source = Fake(); source.videos = { Outcome.Success(emptyList()) }
        val holder = SearchStateHolder(source, wait = {})
        holder.loadMore()
        assertTrue(source.requests.isEmpty())
        holder.search("word", SearchFilterState())
        assertTrue(holder.state.value.end)
        holder.loadMore()
        assertEquals(1, source.requests.size)
    }

    @Test fun userTabLoadsMorePagesInsteadOfStoppingAfterTheFirstScreen() = runBlocking {
        val source = Fake()
        val first = (1..SEARCH_PAGE_SIZE).map { SearchUser(it.toLong(), "u$it", "", "") }
        val second = (1..3).map { SearchUser((SEARCH_PAGE_SIZE + it).toLong(), "v$it", "", "") }
        source.peoplePage = { _, page ->
            Outcome.Success(if (page == 1) first else second)
        }
        val holder = SearchStateHolder(source, wait = {})
        holder.search("word", SearchFilterState())
        assertFalse(holder.state.value.usersEnd)
        holder.loadMoreUsers()
        assertEquals(listOf(1, 2), source.userPages)
        assertEquals(SEARCH_PAGE_SIZE + 3, holder.state.value.users.size)
        assertTrue(holder.state.value.usersEnd)
        assertFalse(holder.state.value.usersLoadingMore)
    }

    @Test fun aNewSearchResetsThePagingCursor() = runBlocking {
        val source = Fake()
        source.videos = { request ->
            if (request.keyword == "old") Outcome.Success((1..SEARCH_PAGE_SIZE).map { video("o$it") })
            else Outcome.Success(listOf(video("new")))
        }
        val holder = SearchStateHolder(source, wait = {})
        holder.search("old", SearchFilterState())
        holder.search("new", SearchFilterState())
        holder.loadMore()
        assertEquals(1, source.requests.last().page)
        assertEquals("new", source.requests.last().keyword)
        assertEquals(1, holder.state.value.results.size)
    }
}
