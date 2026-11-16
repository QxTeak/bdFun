package com.aritxonly.teak.common.player

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kyant.shapes.Capsule
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

@Composable
fun PixelIcon(
    @DrawableRes res: Int,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    contentDescription: String? = null,
) {

    val source = androidx.compose.ui.res.painterResource(res)
    val painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(source)
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = tint?.let { androidx.compose.ui.graphics.ColorFilter.tint(it) },
    )
}

@Composable
fun PauseGlyph(tint: Color = Color.White) {

    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
        Box(Modifier.size(width = 6.dp, height = 22.dp).clip(Capsule()).background(tint))
    }
}

@Composable
fun PlayGlyph(res: Int, tint: Color = Color.White, size: androidx.compose.ui.unit.Dp = 24.dp) {

    PixelIcon(
        res = res,
        tint = tint,
        contentDescription = "播放",
        modifier = Modifier.size(size),
    )
}
