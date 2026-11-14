package com.aritxonly.teak.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import kotlin.math.roundToInt

/** 与 Deadliner 一致：收起时标签不占高度，完整名称由 Tab 的语义节点提供。 */
@Composable
internal fun Modifier.teakDockLabel(layout: TeakFloatingTabLayout): Modifier {
    val fraction = if (layout == TeakFloatingTabLayout.Stacked) {
        1f - LocalTeakDockScrollFraction.current.coerceIn(0f, 1f)
    } else 1f
    return clearAndSetSemantics { }.clipToBounds().layout { measurable, constraints ->
        val label = measurable.measure(constraints.copy(minHeight = 0, maxHeight = Constraints.Infinity))
        layout(label.width, (label.height * fraction).roundToInt()) {
            label.placeRelativeWithLayer(0, 0) { alpha = fraction }
        }
    }
}
