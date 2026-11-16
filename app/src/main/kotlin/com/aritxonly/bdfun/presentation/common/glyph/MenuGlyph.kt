package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*

/** 评论长按菜单的小图标（1.6dp 描边、圆头）。 */
@Composable
fun MenuGlyph(kind: MenuGlyphKind, tint: Color, size: Dp = 18.dp) {
    when (kind) {
        MenuGlyphKind.Reply -> AppGlyph(MiuixIcons.Reply, tint, size = size)
        MenuGlyphKind.Copy -> AppGlyph(MiuixIcons.Copy, tint, size = size)
        MenuGlyphKind.Report -> AppGlyph(MiuixIcons.Report, tint, size = size)
        MenuGlyphKind.Block -> AppGlyph(MiuixIcons.Blocklist, tint, size = size)
        MenuGlyphKind.Delete -> AppGlyph(MiuixIcons.Delete, tint, size = size)
        MenuGlyphKind.Coin -> AppGlyph(LucideIcons.CoinsFilled, tint, size = size)
        MenuGlyphKind.Star -> AppGlyph(LucideIcons.Star, tint, size = size)
    }
}
