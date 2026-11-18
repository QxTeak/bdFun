package com.aritxonly.bdfun

import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal const val DEFAULT_SPACE_BANNER =
    "https://i0.hdslb.com/bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png"

@Composable
internal fun SpaceBanner(bitmap: android.graphics.Bitmap?, aspect: Float = 16f / 10f) {
    val dark = MiuixTheme.colorScheme.background.luminance() < 0.5f
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // 参考图里的头图约 960×600，也就是 16:10；高度跟着屏幕宽度走。
            .aspectRatio(aspect)
            .background(MiuixTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

    }
}

internal suspend fun loadSpaceBannerBitmap(
    context: android.content.Context,
    url: String,
): android.graphics.Bitmap? = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context)
        .data(com.aritxonly.bdfun.core.common.biliImageUrl(url))
        .setHeader("Referer", "https://space.bilibili.com/")
        .size(1440, 1440)
        .scale(coil.size.Scale.FIT)
        .allowHardware(false)
        .build()
    val result = coil.Coil.imageLoader(context).execute(request)
    (result as? coil.request.SuccessResult)?.drawable?.toBitmap()?.takeIf { !it.isRecycled }
}

internal fun bitmapLuminance(bitmap: android.graphics.Bitmap): Float {
    val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 8, 8, false)
    val pixels = IntArray(64)
    small.getPixels(pixels, 0, 8, 0, 0, 8, 8)
    if (small !== bitmap) small.recycle()
    var r = 0L
    var g = 0L
    var b = 0L
    pixels.forEach { p ->
        r += (p shr 16) and 0xFF
        g += (p shr 8) and 0xFF
        b += p and 0xFF
    }
    val n = pixels.size
    return Color((r / n).toInt(), (g / n).toInt(), (b / n).toInt()).luminance()
}
