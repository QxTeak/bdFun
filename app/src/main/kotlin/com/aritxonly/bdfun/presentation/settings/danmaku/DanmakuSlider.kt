package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import com.aritxonly.bdfun.feature.settings.application.*

@Composable
internal fun DmSliderRow(
    label: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, color = DmText, fontSize = 14.sp)
            Spacer(Modifier.weight(1f))
            Text(text = valueText, color = DmSecondary, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        DmSlider(
            value = value,
            range = range,
            onChangeStart = onChangeStart,
            onValueChange = onValueChange,
        )
    }
}

@Composable
internal fun DmSlider(
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
) {
    val span = (range.endInclusive - range.start).coerceAtLeast(0.0001f)
    val ratio = ((value - range.start) / span).coerceIn(0f, 1f)
    val density = LocalDensity.current
    val palette = LocalDanmakuPanelPalette.current
    val thumbRadius = with(density) { 8.dp.toPx() }
    val trackHeight = with(density) { 5.dp.toPx() }
    val tickRadius = with(density) { 1.6.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .let { base ->
                base.then(
                    Modifier.androidPointerSlider(
                        onChangeStart = onChangeStart,
                        onValueChange = {
                            onValueChange(range.start + it * span)
                        },
                    ),
                )
            },
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cy = height / 2f
            val left = thumbRadius
            val usable = (width - thumbRadius * 2f).coerceAtLeast(1f)
            val fillRight = left + usable * ratio
            // 底轨
            drawRoundRect(
                color = palette.track,
                topLeft = Offset(left, cy - trackHeight / 2f),
                size = Size(usable, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            // 已选
            drawRoundRect(
                color = DmPink,
                topLeft = Offset(left, cy - trackHeight / 2f),
                size = Size(usable * ratio, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f),
            )
            // 刻度点
            listOf(0.25f, 0.5f, 0.75f).forEach { mark ->
                drawCircle(
                    color = palette.tick,
                    radius = tickRadius,
                    center = Offset(left + usable * mark, cy),
                )
            }
            // 圆钮
            drawCircle(
                color = palette.thumb,
                radius = thumbRadius,
                center = Offset(fillRight, cy),
            )
        }
    }
}

private fun Modifier.androidPointerSlider(
    onChangeStart: () -> Unit,
    onValueChange: (Float) -> Unit,
): Modifier = this.then(
    Modifier.pointerInput(Unit) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            var start = down.position
            var dragging = false
            var decided = false
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - start.x
                val dy = change.position.y - start.y
                if (!decided) {
                    // 先判断方向：纵向滑动直接放行给外层滚动，避免一碰滑条就改值。
                    if (abs(dy) > viewConfiguration.touchSlop && abs(dy) > abs(dx)) {
                        return@awaitEachGesture
                    }
                    if (abs(dx) > viewConfiguration.touchSlop) {
                        decided = true
                        dragging = true
                        onChangeStart()
                    }
                }
                if (dragging) {
                    onValueChange(
                        (change.position.x / size.width.toFloat().coerceAtLeast(1f))
                            .coerceIn(0f, 1f),
                    )
                    change.consume()
                }
            }
            // 没有拖动、只是点了一下滑轨：按点击位置取值。
            if (!decided) {
                onChangeStart()
                onValueChange(
                    (down.position.x / size.width.toFloat().coerceAtLeast(1f))
                        .coerceIn(0f, 1f),
                )
            }
        }
    },
)
