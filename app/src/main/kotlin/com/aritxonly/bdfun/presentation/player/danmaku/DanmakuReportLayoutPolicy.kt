package com.aritxonly.bdfun.ui

internal data class DanmakuReportLayout(
    val columns: Int,
    val compactButtons: Boolean,
    val maxContentHeightDp: Int?,
    val summaryMaxLines: Int,
)

internal fun danmakuReportLayout(
    landscape: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
): DanmakuReportLayout {
    if (!landscape) {
        return DanmakuReportLayout(
            columns = 3,
            compactButtons = false,
            maxContentHeightDp = null,
            summaryMaxLines = 2,
        )
    }
    val columns = when {
        screenWidthDp >= 600 -> 4
        else -> 3
    }
    return DanmakuReportLayout(
        columns = columns,
        compactButtons = true,
        maxContentHeightDp = (screenHeightDp - 220).coerceIn(80, 180),
        summaryMaxLines = 2,
    )
}
