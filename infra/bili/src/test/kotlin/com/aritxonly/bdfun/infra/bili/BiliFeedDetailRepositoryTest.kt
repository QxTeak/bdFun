package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class BiliFeedDetailRepositoryTest {
    @Test fun detailAndIpKeepOriginalArgumentsAndEmptyValues() = runBlocking {
        var id = ""; var mid = 0L
        val repository = BiliFeedDetailRepository({ id = it; null }, { mid = it; "  " }, { _, _ -> false })
        assertEquals(Outcome.Success(null), repository.detail("1234567890123456789"))
        assertEquals("1234567890123456789", id)
        assertEquals(Outcome.Success("  "), repository.userIpLocation(2283296))
        assertEquals(2283296L, mid)
    }

    @Test fun falseLikeIsSuccessfulTransportWithOriginalPayload() = runBlocking {
        var sent: Pair<String, Boolean>? = null
        val repository = BiliFeedDetailRepository({ null }, { "" }, { id, liked -> sent = id to liked; false })
        assertEquals(Outcome.Success(false), repository.like("one", false))
        assertEquals("one" to false, sent)
    }

    @Test fun cancellationRemainsCancellation() = runBlocking {
        val repository = BiliFeedDetailRepository({ throw CancellationException("leave") }, { "" }, { _, _ -> false })
        try { repository.detail("one"); fail("取消必须传播") } catch (cancel: CancellationException) { assertEquals("leave", cancel.message) }
    }
}
