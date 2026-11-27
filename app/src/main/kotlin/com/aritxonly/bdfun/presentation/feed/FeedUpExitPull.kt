package com.aritxonly.bdfun.ui

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp

/** 下拉退出 UP 页的位移阈值：够长，避免正常回顶被误判成退出。 */
internal val FeedUpExitPullThreshold = 76.dp

/**
 * 下拉退出：在列表顶部继续往下拉，整页跟着手指下移，超过阈值松手即返回动态页。
 *
 * 这一页不刷新（刷新入口只留在分类列表上），所以原来那套下拉刷新连接被替换成它。
 */
internal class FeedUpExitPull(private val thresholdPx: Float, private val onExit: () -> Unit) {
    var offset by mutableFloatStateOf(0f)
        private set
    private var triggered = false

    suspend fun settle() {
        if (offset <= 0f) return
        animate(initialValue = offset, targetValue = 0f, animationSpec = spring(stiffness = 900f)) { value, _ ->
            offset = value
        }
    }

    val connection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
            if (source != NestedScrollSource.UserInput) return Offset.Zero
            // 只认列表顶部的竖直下拉；左右滑 Pager 不参与。
            if (available.y <= 0f || available.x != 0f) return Offset.Zero
            offset = (offset + available.y).coerceAtMost(thresholdPx * 1.3f)
            if (offset >= thresholdPx && !triggered) {
                triggered = true
                offset = 0f
                onExit()
            }
            return Offset(0f, available.y)
        }
    }
}
