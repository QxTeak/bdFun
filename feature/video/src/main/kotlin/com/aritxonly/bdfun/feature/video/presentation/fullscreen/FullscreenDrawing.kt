package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward

@Composable
fun BackChevron(tint: Color = Color.White) {
    AppGlyph(MiuixIcons.ChevronBackward, tint, size = 23.5.dp)
}

@Composable
internal fun MoreGlyph(tint: Color = Color.White) {
    AppGlyph(MiuixIcons.More, tint, size = 20.dp)
}

@Composable
internal fun CloseGlyph(tint: Color = Color.White.copy(alpha = 0.7f)) {
    AppGlyph(MiuixIcons.Close, tint, size = 18.dp)
}

/** 选中勾直接用 Miuix 的图标（Medium 字重和系统列表选中态一致），不再自绘。 */
@Composable
internal fun CheckGlyph(tint: Color = Color.White) {
    androidx.compose.material3.Icon(
        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(MiuixIcons.Medium.Ok),
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = tint,
    )
}

@Composable
internal fun ChevronRightGlyph(tint: Color = Color.White.copy(alpha = 0.4f)) {
    AppGlyph(MiuixIcons.Basic.ArrowRight, tint, size = 14.dp)
}

@Composable
internal fun HeadphoneGlyph(tint: Color = Color.White) {
    AppGlyph(LucideIcons.Headphones, tint, size = 24.dp)
}

@Composable
internal fun ClockGlyph(tint: Color = Color.White) {
    AppGlyph(LucideIcons.Clock, tint, size = 24.dp)
}

@Composable
internal fun PipGlyph(tint: Color = Color.White) {
    AppGlyph(LucideIcons.PictureInPicture2, tint, size = 24.dp)
}

@Composable
internal fun CastGlyph(tint: Color = Color.White) {
    AppGlyph(LucideIcons.Cast, tint, size = 24.dp)
}

/* -------------------------------------------------------------- helpers */
