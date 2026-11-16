package com.aritxonly.teak.common.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ResourceGlyph(
    @DrawableRes res: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    Image(
        painter = rememberAntialiasedIconPainter(painterResource(res)),
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(size),
    )
}
