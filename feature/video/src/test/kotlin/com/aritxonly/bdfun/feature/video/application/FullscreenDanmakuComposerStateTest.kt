package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.video.application.FullscreenDanmakuComposerStateHolder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.Assert.*
import org.junit.Test

class FullscreenDanmakuComposerStateTest {
    @Test fun styledSendKeepsBothOptionsAndPlainColorClearsGradient() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        ui.edit("弹幕")
        ui.toggleVipColor()
        ui.toggleUpIdentity()
        val sent = CompletableDeferred<com.aritxonly.bdfun.core.domain.DanmakuSendAppearance>()
        ui.submitStyled({ _, _, _, _, appearance -> sent.complete(appearance); true }, this, {}, {})
        assertTrue(sent.await().vipColor)
        assertTrue(sent.await().upIdentity)
        ui.setColor(0xFB7299)
        assertFalse(ui.state.value.appearance.vipColor)
        assertTrue(ui.state.value.appearance.upIdentity)
    }

    @Test fun blankInputDoesNotDismissAndMissingSenderOnlyDismisses() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        var dismissed = 0
        var finished = 0
        ui.edit(" \n ")
        ui.submit(null, this, { dismissed++ }, { finished++ })
        assertEquals(0, dismissed)
        ui.edit("hello")
        ui.submit(null, this, { dismissed++ }, { finished++ })
        assertEquals(1, dismissed)
        assertEquals(0, finished)
        assertFalse(ui.state.value.sending)
    }

    @Test fun busyPreventsDuplicateAndKeepsOriginalText() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        val gate = CompletableDeferred<Unit>()
        var calls = 0
        var submitted = ""
        var finished = 0
        val sender: suspend (String, Int, Int, Int) -> Boolean = { text, _, _, _ ->
            calls++; submitted = text; gate.await(); true
        }
        ui.edit("  hello  ")
        ui.submit(sender, this, {}, { finished++ })
        ui.submit(sender, this, {}, { finished++ })
        yield()
        assertTrue(ui.state.value.sending)
        assertEquals(1, calls)
        assertEquals("hello", submitted)
        gate.complete(Unit)
        yield()
        assertFalse(ui.state.value.sending)
        assertEquals(1, finished)
        assertEquals("  hello  ", ui.state.value.text)
    }

    @Test fun optionsAreReadWhenSendCoroutineRunsAndFailureStillFinishes() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        var observed = emptyList<Int>()
        var finished = 0
        ui.edit("hello")
        ui.submit({ _, mode, size, color ->
            observed = listOf(mode, size, color)
            error("request failed")
        }, this, {}, { finished++ })
        ui.setMode(5)
        ui.setFontSize(18)
        ui.setColor(0xFB7299)
        yield()
        assertEquals(listOf(5, 18, 0xFB7299), observed)
        assertFalse(ui.state.value.sending)
        assertEquals(1, finished)
        assertEquals("hello", ui.state.value.text)
    }

    @Test fun falseResponseStillDismissesAndPreservesDraft() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        var finished = 0
        ui.edit("draft")
        ui.submit({ _, _, _, _ -> false }, this, {}, { finished++ })
        yield()
        assertEquals(1, finished)
        assertEquals("draft", ui.state.value.text)
        assertFalse(ui.state.value.sending)
    }

    @Test fun closingBeforeDispatchDoesNotSendOrDismiss() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        var calls = 0
        ui.edit("draft")
        ui.submit({ _, _, _, _ -> calls++; true }, this, {}, { calls++ })
        ui.close()
        yield()
        ui.edit("late")
        assertEquals(0, calls)
        assertEquals("draft", ui.state.value.text)
    }

    @Test fun lateCompletionAfterCloseDoesNotDismiss() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        val gate = CompletableDeferred<Unit>()
        var finished = 0
        ui.edit("draft")
        ui.submit({ _, _, _, _ -> gate.await(); true }, this, {}, { finished++ })
        yield()
        ui.close()
        gate.complete(Unit)
        yield()
        assertEquals(0, finished)
    }

    @Test fun cancellationReleasesBusyWithoutDismissal() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        val owner = Job()
        val scope = CoroutineScope(coroutineContext + owner)
        var finished = 0
        ui.edit("draft")
        ui.submit({ _, _, _, _ -> CompletableDeferred<Unit>().await(); true }, scope, {}, { finished++ })
        yield()
        owner.cancel()
        yield()
        assertFalse(ui.state.value.sending)
        assertEquals(0, finished)
    }

    @Test fun alreadyCancelledScopeDoesNotLeaveBusy() = runBlocking {
        val ui = FullscreenDanmakuComposerStateHolder()
        val owner = Job().also { it.cancel() }
        ui.edit("draft")
        ui.submit({ _, _, _, _ -> error("must not run") }, CoroutineScope(coroutineContext + owner), {}, {})
        yield()
        assertFalse(ui.state.value.sending)
    }
}
