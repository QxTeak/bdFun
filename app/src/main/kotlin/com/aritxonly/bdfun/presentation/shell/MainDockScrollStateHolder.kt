package com.aritxonly.bdfun

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/** 与 Deadliner 共用方向切换规则；只累计列表实际消费的手势，避免翻页和边界拖动误触发。 */
internal class MainDockScrollStateHolder(private val switchDistancePx: Float) : NestedScrollConnection {
    var collapsed by mutableStateOf(false)
        private set
    private var travel = 0f

    override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
        if (source == NestedScrollSource.UserInput && consumed.y != 0f) {
            if (travel * consumed.y < 0f) travel = 0f
            travel = (travel + consumed.y).coerceIn(-switchDistancePx, switchDistancePx)
            if (kotlin.math.abs(travel) >= switchDistancePx) collapsed = travel < 0f
        }
        return Offset.Zero
    }
}
