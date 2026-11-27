package com.aritxonly.bdfun.ui

/**
 * 举报弹幕弹窗的自适应布局参数。
 *
 * 横屏/矮屏压缩按钮与文案行数，避免弹窗超过可视高度；普通竖屏保持三列。
 * 单独放在这里而不是塞进 Composable 里，方便以后加纯 JVM 测试。
 */
internal data class DanmakuReportLayout(
    val compactButtons: Boolean,
    val maxContentHeightDp: Int?,
    val summaryMaxLines: Int,
    val columns: Int,
)

internal fun danmakuReportLayout(
    landscape: Boolean,
    screenWidthDp: Int,
    screenHeightDp: Int,
): DanmakuReportLayout {
    val compact = landscape || screenHeightDp < 640 || screenWidthDp < 360
    return DanmakuReportLayout(
        compactButtons = compact,
        maxContentHeightDp = if (compact) (screenHeightDp - 240).coerceIn(120, 320) else null,
        summaryMaxLines = if (compact) 2 else 3,
        columns = if (screenWidthDp < 340) 2 else 3,
    )
}
