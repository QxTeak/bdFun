package com.aritxonly.bdfun.feature.settings.application

/** 分隔条不计入3:7内容比例；优先保障左栏360dp，右栏始终使用剩余宽度。 */
internal object SettingsSplitPolicy {
    const val DefaultFraction = .3f
    const val MinimumLeftDp = 360f
    const val DividerDp = 12f

    fun leftWidth(totalWidth: Float, fraction: Float = DefaultFraction): Float {
        val available = (totalWidth - DividerDp).coerceAtLeast(0f)
        val minimum = MinimumLeftDp.coerceAtMost(available)
        val maximum = (available - 1f).coerceAtLeast(minimum)
        val safeFraction = if (fraction.isFinite()) fraction.coerceIn(0f, 1f) else DefaultFraction
        return (available * safeFraction).coerceIn(minimum, maximum)
    }

    fun drag(totalWidth: Float, fraction: Float, deltaDp: Float): Float {
        val available = totalWidth - DividerDp
        if (available <= 0f || !deltaDp.isFinite()) return fraction
        val target = leftWidth(totalWidth, fraction) + deltaDp
        return leftWidth(totalWidth, target / available) / available
    }
}
