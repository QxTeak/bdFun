package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward

@Composable
internal fun InlineBackGlyph() {
    AppGlyph(MiuixIcons.ChevronBackward, Color.White, size = 20.dp)
}

/** 首页与其他入口共用默认 Regular。 */
@Composable
internal fun InlineHomeGlyph(tint: Color = Color.White) {
    AppGlyph(MiuixIcons.Home, tint, size = 21.dp)
}

/** 相同耳机语义共用同一 Lucide 资源。 */
@Composable
internal fun InlineHeadphoneGlyph() {
    AppGlyph(LucideIcons.Headphones, Color.White, size = 22.dp)
}

/** 半屏投屏保留预设选中的 ScreenMirroring，与全屏 Cast 区分。 */
@Composable
internal fun InlineCastGlyph() {
    AppGlyph(MiuixIcons.ScreenMirroring, Color.White, size = 23.dp)
}

@Composable
internal fun InlineMoreGlyph(tint: Color = Color.White) {
    AppGlyph(MiuixIcons.More, tint, size = 22.dp)
}
