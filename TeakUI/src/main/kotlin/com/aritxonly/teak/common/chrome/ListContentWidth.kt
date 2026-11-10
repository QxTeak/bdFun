package com.aritxonly.teak.common.chrome

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Center readable content in a full-width viewport. Sharing the list's scroll state lets
 * drags and flings in either gutter scroll the same list.
 */
fun Modifier.centeredListWidth(
    maxWidth: Dp? = null,
    scrollState: ScrollableState? = null,
): Modifier = this
    .then(
        if (scrollState != null) Modifier.scrollable(
            state = scrollState,
            orientation = Orientation.Vertical,
            reverseDirection = true,
        ) else Modifier,
    )
    .layout { measurable, constraints ->
        val contentWidth = when {
            maxWidth != null -> minOf(constraints.maxWidth, maxWidth.roundToPx())
            !constraints.hasBoundedWidth -> 680.dp.roundToPx()
            constraints.maxWidth >= 600.dp.roundToPx() -> minOf((constraints.maxWidth * 0.7f).roundToInt(), 680.dp.roundToPx())
            else -> constraints.maxWidth
        }
        val child = measurable.measure(constraints.copy(
            minWidth = constraints.minWidth.coerceAtMost(contentWidth),
            maxWidth = contentWidth,
        ))
        val viewportWidth = if (constraints.hasBoundedWidth) constraints.maxWidth else child.width
        layout(viewportWidth, child.height) {
            child.placeRelative((viewportWidth - child.width) / 2, 0)
        }
}
