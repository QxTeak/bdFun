package com.aritxonly.bdfun.feature.sent.application

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatVideoTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%02d:%02d".format(m, s)
    }
}

// 本机发送记录一次最多 500 条，逐条新建 SimpleDateFormat 在滚动时很贵。
private val sentAtFormatter = ThreadLocal.withInitial {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
}

internal fun formatSentAt(ms: Long): String =
    sentAtFormatter.get()!!.format(Date(ms))
