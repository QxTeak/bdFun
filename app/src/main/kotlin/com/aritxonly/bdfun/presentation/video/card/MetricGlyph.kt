package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun MetricGlyph(
    play: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 13.dp,
) {
    Image(
        painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(androidx.compose.ui.res.painterResource(
            if (play) TeakIcon.MetricPlay else TeakIcon.MetricDanmaku,
        )),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
        modifier = modifier.size(size),
    )
}
