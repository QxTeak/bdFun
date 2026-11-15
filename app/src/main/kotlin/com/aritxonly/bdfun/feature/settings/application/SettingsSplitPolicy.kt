package com.aritxonly.bdfun.feature.settings.application

/** 固定3:7，无分隔线；窄横屏优先保障一级列表360dp。 */
internal object SettingsSplitPolicy {
    const val DefaultFraction = .3f
    const val MinimumLeftDp = 360f
    fun leftWidth(totalWidth: Float): Float =
        (totalWidth * DefaultFraction).coerceAtLeast(MinimumLeftDp).coerceAtMost(totalWidth.coerceAtLeast(0f))
}
