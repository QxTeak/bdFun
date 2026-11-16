package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Lock
import top.yukonga.miuix.kmp.icon.extended.Unlock

fun describeMediaUrl(url: String?): String {
    if (url.isNullOrBlank()) return "none"
    return runCatching {
        val uri = android.net.Uri.parse(url)
        "${uri.host}${uri.path}"
    }.getOrDefault("invalid")
}

/** Fullscreen lock glyph: shackle closed when locked, open when unlocked. */
@Composable
fun LockGlyph(locked: Boolean) {
    AppGlyph(if (locked) MiuixIcons.Lock else MiuixIcons.Unlock, Color.White, size = 23.dp)
}

internal fun sponsorCategoryLabel(category: String): String = when (category) {
    "sponsor" -> "赞助"
    "selfpromo" -> "自我推广"
    "interaction" -> "互动提醒"
    "intro" -> "开场"
    "outro" -> "结尾"
    "preview" -> "预览"
    "music_offtopic" -> "非音乐部分"
    "filler" -> "填充"
    else -> "片段"
}
