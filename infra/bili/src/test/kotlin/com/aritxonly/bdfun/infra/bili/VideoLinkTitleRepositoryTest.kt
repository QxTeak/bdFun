package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.model.video.ResolvedVideoLink
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class VideoLinkTitleRepositoryTest {
    private val bv = "BV1orW46aEba"
    @Test fun aidResolutionStillLoadsBvidThenLoadsItsTitleOnIo() = runBlocking {
        val caller = Thread.currentThread(); val calls = mutableListOf<String>()
        val repository = BiliVideoLinkTitleRepository({ calls += "title:$it"; assertNotSame(caller, Thread.currentThread()); "标题" },
            { calls += "aid:$it"; bv })
        assertEquals(Outcome.Success(ResolvedVideoLink(bv, "标题")), repository.resolve("av123"))
        assertEquals(listOf("aid:123", "title:$bv"), calls)
    }
    @Test fun bareAndUrlBvidCaseFollowTheOriginalBranches() = runBlocking {
        val seen = mutableListOf<String>(); val repository = BiliVideoLinkTitleRepository({ seen += it; "title" }, { error("aid") })
        repository.resolve("bv" + bv.substring(2)); repository.resolve("https://www.bilibili.com/video/bv" + bv.substring(2))
        assertEquals(listOf(bv, "bv" + bv.substring(2)), seen)
    }
    @Test fun fullAidUrlAndUppercaseShortHostResolveMetadata() = runBlocking {
        val aids = mutableListOf<Long>(); val shorts = mutableListOf<String>()
        val repository = BiliVideoLinkTitleRepository({ "标题" }, { aids += it; bv }, { shorts += it; bv })
        assertEquals(Outcome.Success(ResolvedVideoLink(bv, "标题")),
            repository.resolve("https://www.bilibili.com/video/av123"))
        assertEquals(listOf(123L), aids)
        assertEquals(Outcome.Success(ResolvedVideoLink(bv, "标题")), repository.resolve("HTTPS://B23.TV/AB"))
        assertEquals(listOf("HTTPS://B23.TV/AB"), shorts)
    }
    @Test fun shortLinksAndBlankResultsUseOriginalFallback() = runBlocking {
        val seen = mutableListOf<String>(); val repository = BiliVideoLinkTitleRepository({ " " }, { " " }, { seen += it; bv })
        assertEquals(Outcome.Success(null), repository.resolve("https://b23.tv/A")); assertEquals(listOf("https://b23.tv/A"), seen)
        assertEquals(Outcome.Success(null), repository.resolve("av123"))
    }
    @Test fun repositoryFailuresAreTypedAndCancellationEscapes() = runBlocking {
        val offline = BiliVideoLinkTitleRepository({ throw IOException("offline") }, { bv })
        assertTrue((offline.resolve(bv) as Outcome.Failure).error is AppError.Network)
        val cancelled = BiliVideoLinkTitleRepository({ throw CancellationException("cancel") }, { bv })
        try { cancelled.resolve(bv); fail("cancellation expected") } catch (_: CancellationException) { }
    }
}
