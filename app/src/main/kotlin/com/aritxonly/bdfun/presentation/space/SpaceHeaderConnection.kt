package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
internal fun rememberSpaceHeaderConnection(
    header: SpaceHeaderStateHolder, headerMaxCollapsePx: Float,
): androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
    return remember(headerMaxCollapsePx) {
        val maxCollapse = headerMaxCollapsePx
        object : androidx.compose.ui.input.nestedscroll.NestedScrollConnection {
            override fun onPreScroll(
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                // 只跟手指走：惯性滚动不再被头部整块吃掉，否则一甩就会出现“突然停住”的怪手感。
                if (source != androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                    return androidx.compose.ui.geometry.Offset.Zero
                }
                if (available.y < 0f && header.collapsePx < maxCollapse) {
                    val consume = (-available.y).coerceAtMost(maxCollapse - header.collapsePx)
                    header.collapsePx += consume
                    return androidx.compose.ui.geometry.Offset(0f, -consume)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }

            override fun onPostScroll(
                consumed: androidx.compose.ui.geometry.Offset,
                available: androidx.compose.ui.geometry.Offset,
                source: androidx.compose.ui.input.nestedscroll.NestedScrollSource,
            ): androidx.compose.ui.geometry.Offset {
                if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput &&
                    available.y > 0f && header.collapsePx > 0f
                ) {
                    val consume = available.y.coerceAtMost(header.collapsePx)
                    header.collapsePx -= consume
                    return androidx.compose.ui.geometry.Offset(0f, consume)
                }
                return androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }
}
