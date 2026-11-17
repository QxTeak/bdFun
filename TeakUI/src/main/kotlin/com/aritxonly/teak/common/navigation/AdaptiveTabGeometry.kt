package com.aritxonly.teak.common.navigation

internal fun normalizedTabLimit(value: Int): Int = value.coerceAtLeast(1).let {
    if (it % 2 == 0) it - 1 else it
}

/** 露字占用现有宽度，不添加首尾占位；边界自然贴合原胶囊。 */
internal data class AdaptiveTabGeometry(val count: Int, val viewport: Float, val item: Float) {
    val maxScroll: Float get() = (count * item - viewport).coerceAtLeast(0f)
    fun scroll(position: Float): Float =
        ((position.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat()) + .5f) * item - viewport / 2)
            .coerceIn(0f, maxScroll)
    fun indicator(position: Float): Float = position * item - scroll(position)
}
