package com.aritxonly.teak.common.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 两个来源使用同一尺寸入口；图形大小不承担按钮的触摸区域。 */
@Composable
fun AppGlyph(
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Icon(rememberAntialiasedIconPainter(rememberVectorPainter(icon)), contentDescription, modifier.size(size), tint)
}

@Composable
fun AppGlyph(
    @DrawableRes icon: Int,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 22.dp,
    contentDescription: String? = null,
) {
    Icon(rememberAntialiasedIconPainter(androidx.compose.ui.res.painterResource(icon)), contentDescription, modifier.size(size), tint)
}
