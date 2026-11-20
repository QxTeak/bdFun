package com.aritxonly.bdfun

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun sameChatDay(a: Long, b: Long): Boolean {
    if (a <= 0L || b <= 0L) return a == b
    val zone = ZoneId.systemDefault()
    return Instant.ofEpochSecond(a).atZone(zone).toLocalDate() ==
        Instant.ofEpochSecond(b).atZone(zone).toLocalDate()
}

// DateTimeFormatter 不可变、线程安全；聊天列表逐条渲染时不再新建 SimpleDateFormat/Calendar。
private val chatTimeThisYear = DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)
private val chatTimeOtherYear = DateTimeFormatter.ofPattern("yyyy年M月d日 HH:mm", Locale.CHINA)

internal fun formatChatTime(seconds: Long): String {
    if (seconds <= 0L) return ""
    val zone = ZoneId.systemDefault()
    val zoned = Instant.ofEpochSecond(seconds).atZone(zone)
    val formatter = if (zoned.year == LocalDate.now(zone).year) chatTimeThisYear else chatTimeOtherYear
    return formatter.format(zoned)
}
