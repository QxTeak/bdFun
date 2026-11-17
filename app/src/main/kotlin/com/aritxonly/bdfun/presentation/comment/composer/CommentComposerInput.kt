package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.teakContentControlShadow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule

@Composable
internal fun androidx.compose.foundation.layout.RowScope.CommentComposerInput(
    content: CommentComposerContent, actions: CommentComposerActions, material: CommentComposerMaterial,
) {
    val recipe = material.recipe
    val backdrop = material.backdrop
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    TeakGlassSurface(
        modifier = Modifier.weight(1f).heightIn(min = 46.dp, max = 160.dp)
            .then(if (classic) Modifier else Modifier.teakContentControlShadow(Capsule())),
        shape = Capsule(),
        recipe = recipe,
        backdrop = backdrop,
        flatBackgroundColor = if (classic) {
            top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surfaceContainerHigh
        } else {
            null
        },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CommentComposerTextField(content, actions)
            CommentComposerActionRow(content, actions)
        }
    }

}
