package com.android.purebilibili.danmaku.engine

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import com.bytedance.danmaku.render.engine.DanmakuView
import com.bytedance.danmaku.render.engine.control.DanmakuCommand
import com.bytedance.danmaku.render.engine.control.ICommandMonitor
import com.bytedance.danmaku.render.engine.render.draw.text.TextData
import com.bytedance.danmaku.render.engine.render.draw.text.TextDrawItem
import com.bytedance.danmaku.render.engine.utils.CMD_PAUSE_ITEM
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DanmakuTrailingActionTest {
    @Test
    fun trailingButtonReservesWidthAndDispatchesWithoutPausingTheItem() {
        val view = DanmakuView(RuntimeEnvironment.getApplication())
        val engine = ByteDanceDanmakuEngine(view)
        try {
            val config = DanmakuRenderConfig(textSizePx = 20f, lineHeightPx = 40f, lineCount = 1)
            engine.updateConfig(config)
            view.layout(0, 0, 500, 300)
            val tapped = mutableListOf<Long>()
            val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
            var liked = false
            val action = DanmakuTrailingAction(bitmap, Color.WHITE, Color.YELLOW, { liked }) { id ->
                tapped += id
                liked = !liked
            }
            val plain = TextDrawItem().apply {
                bindData(TextData().apply { text = "Test" })
                measure(view.controller.config)
            }

            var menuBounds: RectF? = null
            engine.setOnItemClickListener { _, rect, _ -> menuBounds = RectF(rect) }
            var pauses = 0
            view.controller.registerCmdMonitor(object : ICommandMonitor {
                override fun onCommand(cmd: DanmakuCommand) {
                    if (cmd.what == CMD_PAUSE_ITEM) pauses++
                }
            })
            engine.start()
            engine.addImmediate(DanmakuItem().apply {
                danmakuId = 42L
                text = "Test"
                trailingAction = action
            })
            val frame = Bitmap.createBitmap(500, 300, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(frame)
            view.controller.draw(view, canvas)
            repeat(10) {
                ShadowSystemClock.advanceBy(Duration.ofMillis(100))
                view.controller.draw(view, canvas)
            }

            assertTrue(plain.width > 0f)
            val fullWidth = plain.width + DanmakuTrailingAction.advance(plain.height)
            val bodyX = 500f - (500f + fullWidth) * 1_000f / config.scrollDurationMs + plain.width / 2f
            assertNotNull(engine.captureClick(bodyX, plain.height / 2f)).invoke()
            val rect = assertNotNull(menuBounds)
            assertEquals(plain.width + DanmakuTrailingAction.advance(plain.height), rect.width(), 0.01f)
            assertEquals(1, pauses)
            engine.clearClickSelection()
            menuBounds = null

            val buttonX = rect.right - DanmakuTrailingAction.advance(rect.height()) / 2f
            assertEquals(Color.WHITE, frame.getPixel(buttonX.toInt(), rect.centerY().toInt()))
            val cancelled = assertNotNull(engine.captureClick(buttonX, rect.centerY()))
            cancelled.cancel()
            cancelled()
            assertTrue(tapped.isEmpty())
            val press = assertNotNull(engine.captureClick(buttonX, rect.centerY()))
            press()
            press()
            assertEquals(listOf(42L), tapped)
            assertEquals(null, menuBounds)
            assertEquals(1, pauses)

            frame.eraseColor(Color.TRANSPARENT)
            view.controller.draw(view, canvas)
            assertEquals(Color.YELLOW, frame.getPixel(buttonX.toInt(), rect.centerY().toInt()))
        } finally {
            engine.close()
        }
    }
}
