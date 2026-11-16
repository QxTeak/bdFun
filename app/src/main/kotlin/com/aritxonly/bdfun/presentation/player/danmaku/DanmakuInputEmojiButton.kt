package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.feature.video.application.*
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.aritxonly.teak.common.material.TeakGlassIconButton

@Composable
internal fun DanmakuInputEmojiButton(runtime: DanmakuInputRuntime) {
    val focusRequester = runtime.focusRequester
    val recipe = runtime.recipe
    val backdrop = runtime.backdrop
    TeakGlassIconButton(
        onClick = {
            runtime.ui.showEmoji = !runtime.ui.showEmoji
            runCatching { focusRequester.requestFocus() }
        },
        size = 46.dp,
        recipe = recipe,
        backdrop = backdrop,
    ) {
        AppGlyph(LucideIcons.FaceGrinning, size = 19.dp, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
    }
}
