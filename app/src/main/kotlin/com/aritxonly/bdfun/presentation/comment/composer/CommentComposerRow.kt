package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.aritxonly.teak.common.material.TeakGlassIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentComposerRow(content: CommentComposerContent, actions: CommentComposerActions, material: CommentComposerMaterial) {
    val recipe = material.recipe
    val backdrop = material.backdrop
    Row(verticalAlignment = Alignment.CenterVertically) {
        CommentComposerInput(content, actions, material)
        Spacer(Modifier.width(8.dp))
        TeakGlassIconButton(
            onClick = actions::toggleEmoji,
            size = 46.dp,
            recipe = recipe,
            backdrop = backdrop,
        ) {
            AppGlyph(LucideIcons.FaceGrinning, size = 19.dp, tint = MiuixTheme.colorScheme.onBackgroundVariant)
        }
    }

}
