package com.aritxonly.bdfun.infra.playback

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], manifest = Config.NONE)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MediaArtworkTest {
    @Test fun rectangularCoverHasTransparentCornersAndKeepsSourceUntouched() {
        val source = Bitmap.createBitmap(160, 90, Bitmap.Config.ARGB_8888)
        source.eraseColor(Color.BLUE)
        val artwork = mediaArtwork(source)
        assertEquals(90, artwork.width)
        assertEquals(90, artwork.height)
        assertEquals(0, Color.alpha(artwork.getPixel(0, 0)))
        assertEquals(Color.BLUE, artwork.getPixel(45, 45))
        assertEquals(Color.BLUE, source.getPixel(0, 0))
    }
}
