package com.aritxonly.bdfun.ui

import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test

class PlayerGestureSessionTest {
    private fun callbacks(log: MutableList<String>) = GestureCallbacks(
        onTap = { log += "tap" }, onDoubleTap = { log += "double" },
        onLongPressStart = { log += "long" }, onLongPressEnd = { log += "long-end" },
        onDragStart = { left, horizontal, allowed -> log += "start:$left:$horizontal:$allowed" },
        onDrag = { _, dy -> log += "vertical:$dy" }, onHorizontalDrag = { log += "horizontal:$it" },
        onDragEnd = { log += "end" }, ignoreDown = { _, _ -> false },
        swipe = GestureSwipeCallbacks(
            centerSwipeEnabled = true, onCenterSwipe = { log += "center" },
            boostSwipeEnabled = true, onBoostSwipe = { log += "boost:$it" },
        ),
    )

    @Test fun danmakuSingleTapConfirmsInsteadOfTogglingControls() = runBlocking {
        val log = mutableListOf<String>()
        val taps = PlayerTapStateHolder()
        val cb = callbacks(log).copy(captureDanmaku = {
            PlayerDanmakuPress({ log += "danmaku" }, { log += "release" })
        })
        val gesture = PlayerGestureSession(Offset.Zero, { 200f }, { 10f }, false, false, true,
            this, taps, { cb }, { 10000L })
        val job = gesture.startLongPress()
        gesture.released(); job.cancel(); gesture.finish(70f)
        taps.pending!!.join()
        assertEquals(listOf("danmaku"), log)
    }

    @Test fun dragReleasesDanmakuWithoutOpeningMenu() = runBlocking {
        val log = mutableListOf<String>()
        val cb = callbacks(log).copy(captureDanmaku = {
            PlayerDanmakuPress({ log += "danmaku" }, { log += "release" })
        })
        val gesture = PlayerGestureSession(Offset.Zero, { 200f }, { 10f }, false, false, true,
            this, PlayerTapStateHolder(), { cb })
        val job = gesture.startLongPress()
        gesture.move(Offset(20f, 0f)) {}
        gesture.released(); job.cancel(); gesture.finish(70f)
        assertEquals(1, log.count { it == "release" })
        assertFalse(log.contains("danmaku"))
    }

    @Test fun horizontalDragUsesCurrentCallbackAndLiveWidth() = runBlocking {
        val log = mutableListOf<String>()
        var width = 200f
        var current = callbacks(log)
        val gesture = PlayerGestureSession(Offset.Zero, { width }, { 10f }, true, true, false,
            this, PlayerTapStateHolder(), { current })
        val job = gesture.startLongPress()
        var consumed = 0
        gesture.move(Offset(10f, 0f)) { consumed++ }
        assertTrue(log.isEmpty())
        current = current.copy(onHorizontalDrag = { log += "latest:$it" })
        width = 400f
        gesture.move(Offset(20f, 0f)) { consumed++ }
        job.cancel()
        gesture.finish(70f)
        assertEquals(listOf("start:true:true:true", "latest:0.05", "end"), log)
        assertEquals(1, consumed)
    }

    @Test fun centerSwipeRequiresStrictThresholdAndDoesNotBeginVerticalDrag() = runBlocking {
        val log = mutableListOf<String>()
        fun session() = PlayerGestureSession(Offset.Zero, { 200f }, { 10f }, false, false, true,
            this, PlayerTapStateHolder(), { callbacks(log) })
        val first = session()
        val firstJob = first.startLongPress()
        first.move(Offset(0f, 70f)) {}
        firstJob.cancel(); first.finish(70f)
        assertTrue(log.isEmpty())
        val second = session()
        val secondJob = second.startLongPress()
        second.move(Offset(0f, -71f)) {}
        secondJob.cancel(); second.finish(70f)
        assertEquals(listOf("center"), log)
    }

    @Test fun secondShortReleaseCancelsPendingSingleTap() = runBlocking {
        val log = mutableListOf<String>()
        val taps = PlayerTapStateHolder()
        var time = 10000L
        fun session() = PlayerGestureSession(Offset.Zero, { 200f }, { 10f }, false, false, true,
            this, taps, { callbacks(log) }, { time })
        val first = session()
        val firstJob = first.startLongPress()
        time += 50; first.released(); firstJob.cancel(); first.finish(70f)
        time += 50
        val second = session()
        val secondJob = second.startLongPress()
        time += 50; second.released(); secondJob.cancel(); second.finish(70f)
        assertEquals(listOf("double"), log)
        assertNull(taps.pending)
    }
    @Test fun longPressConsumesMovementWithoutEnteringDragAndEndsOnce() = runBlocking {
        val log = mutableListOf<String>()
        val started = CompletableDeferred<Unit>()
        var current = callbacks(log).copy(onLongPressStart = { log += "long"; started.complete(Unit) })
        val gesture = PlayerGestureSession(Offset.Zero, { 200f }, { 10f }, true, true, false,
            this, PlayerTapStateHolder(), { current })
        val job = gesture.startLongPress()
        withTimeout(5000) { started.await() }
        var consumed = 0
        gesture.move(Offset(40f, 50f)) { consumed++ }
        current = current.copy(swipe = current.swipe.copy(boostSwipeEnabled = false))
        gesture.move(Offset(60f, 100f)) { consumed++ }
        gesture.released()
        job.cancel(); gesture.finish(70f)
        assertEquals(listOf("long", "boost:0.2", "long-end"), log)
        assertEquals(2, consumed)
    }
}
