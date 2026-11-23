package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.SpaceSignToggle(
    ui: SpaceSignStateHolder,
    progress: Float,
) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .size(20.dp)
            .clickable(interactionSource = null, indication = null) {
                ui.expanded = !ui.expanded
            },
        contentAlignment = Alignment.Center,
    ) {
        // 视频页同款 ChevronForward：向下 90°，展开过程中再转 180°；尺寸比视频页小。
        AppGlyph(
            MiuixIcons.ChevronForward,
            tint = colors.onBackgroundVariant,
            size = 12.dp,
            modifier = Modifier.graphicsLayer { rotationZ = 90f + 180f * progress },
        )
    }
}
