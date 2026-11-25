package com.aritxonly.bdfun.ui

internal data class DanmakuReportLayout(
    val columns: Int,
    val compactButtons: Boolean,
    val maxContentHeightDp: Int?,
)

internal fun danmakuReportLayout(
    landscape: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
): DanmakuReportLayout {
    if (!landscape) return DanmakuReportLayout(columns = 3, compactButtons = false, maxContentHeightDp = null)
    val columns = when {
        screenWidthDp >= 720 -> 5
        screenWidthDp >= 600 -> 4
        else -> 3
    }
    return DanmakuReportLayout(
        columns = columns,
        compactButtons = true,
        maxContentHeightDp = (screenHeightDp - 190).coerceIn(140, 240),
    )
}
