package com.aritxonly.bdfun.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/* ---------------------------------------------------------------- time */

// DateTimeFormatter 不可变、线程安全，可以直接复用；避免每一行消息都新建 SimpleDateFormat/Calendar。
private val sessionTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
private val sessionMonthDayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val sessionFullDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日", Locale.CHINA)
private val messageDateFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)

internal fun formatSessionTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val instant = Instant.ofEpochSecond(seconds)
    val date = instant.atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when {
        date == today -> sessionTimeFormatter.format(instant.atZone(zone))
        date == today.minusDays(1) ->
            "昨天 " + sessionTimeFormatter.format(instant.atZone(zone))
        date.year == today.year -> sessionMonthDayFormatter.format(date)
        else -> sessionFullDateFormatter.format(date)
    }
}

internal fun formatMessageDate(seconds: Long): String {
    if (seconds <= 0L) return ""
    val zone = ZoneId.systemDefault()
    return messageDateFormatter.format(Instant.ofEpochSecond(seconds).atZone(zone))
}
