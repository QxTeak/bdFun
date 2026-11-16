package com.aritxonly.bdfun.feature.update.application

import com.aritxonly.bdfun.core.common.*
import com.aritxonly.bdfun.core.domain.update.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class UpdateStateHolderTest {
    @Test fun channelAndUpgradeRules() = runBlocking {

        val repository = object : UpdateRepository {
            override suspend fun releases(): Outcome<List<UpdateRelease>> {
                return Outcome.Success(listOf(UpdateRelease(AppVersion.parse("1.1.0")!!, "日志", "", null)))
            }
        }
        val old = UpdateStateHolder(repository, AppVersion.parse("preview-5.0")!!)
        old.check()
        assertNotNull(old.state.value.release)
        val same = UpdateStateHolder(repository, AppVersion.parse("1.1.0")!!)
        same.check()
        assertNull(same.state.value.release)
        assertTrue(same.state.value.checked)
        assertEquals("日志", same.state.value.currentRelease?.notes)
        same.showHistory(true)
        assertTrue(same.state.value.history)
    }
    @Test fun cancellationReleasesBusy() = runBlocking {
        val holder = UpdateStateHolder(object : UpdateRepository {
            override suspend fun releases(): Outcome<List<UpdateRelease>> = throw CancellationException()
        }, AppVersion.parse("preview-5.0")!!)
        try { holder.check(); fail("must propagate cancellation") } catch (_: CancellationException) { }
        assertFalse(holder.state.value.checking)
    }
}
