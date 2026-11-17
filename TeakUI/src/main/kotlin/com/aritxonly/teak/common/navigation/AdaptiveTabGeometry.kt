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
    /** 中间提示向内收；端点减少另一端的露字量，不改变槽位或选中指示器。 */
    fun edgeInset(index: Int, position: Float): Float {
        val center = (index + .5f) * item - scroll(position)
        val sign = if (center < viewport / 2f) 1f else -1f
        val endpoint = if (sign < 0f) (1f - position).coerceIn(0f, 1f)
            else (position - (count - 2)).coerceIn(0f, 1f)
        return sign * edgeFade(index, position) * item * (.22f - .30f * endpoint)
    }

    fun edgeFade(index: Int, position: Float): Float {
        val offset = scroll(position)
        val center = (index + .5f) * item - offset
        val distance = minOf(if (offset > .5f) center else Float.POSITIVE_INFINITY,
            if (offset < maxScroll - .5f) viewport - center else Float.POSITIVE_INFINITY)
        return (1f - distance / (item * .6f)).coerceIn(0f, 1f)
    }
}

/** 左滑推进，右滑返回；不做橡皮筋，端点不会出现空白。 */
internal fun draggedTabPosition(position: Float, delta: Float, item: Float, count: Int): Float =
    (position - delta / item.coerceAtLeast(1f)).coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())

/** 快划最多多推进一个选项，慢拖按距离吸附，避免轻扫跨过数个频道。 */
internal fun settledTabIndex(position: Float, velocity: Float, item: Float, count: Int): Int {
    val projected = position - (velocity * .12f / item.coerceAtLeast(1f)).coerceIn(-.65f, .65f)
    return kotlin.math.floor(projected + .5f).toInt().coerceIn(0, (count - 1).coerceAtLeast(0))
}
