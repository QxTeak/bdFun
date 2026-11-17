package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule

@Composable
internal fun PrivateChatComposerRow(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions, material: PrivateChatComposerMaterial,
) {
    val onToggleEmoji = actions.onToggleEmoji
    val recipe = material.recipe
    val backdrop = material.backdrop
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Row(verticalAlignment = Alignment.CenterVertically) {
        TeakGlassSurface(
            modifier = Modifier.weight(1f).height(46.dp),
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
            PrivateChatComposerInput(content, actions)
        }
        Spacer(Modifier.width(8.dp))
        TeakGlassIconButton(
            onClick = onToggleEmoji,
            size = 46.dp,
            recipe = recipe,
            backdrop = backdrop,
        ) {
            Text(text = "\uD83D\uDE00", fontSize = 19.sp)
        }
    }
}
