package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.material.teakGlassControlShadow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.material.TeakGlassSurface
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.kyant.shapes.Capsule

@Composable
internal fun androidx.compose.foundation.layout.RowScope.CommentComposerInput(
    content: CommentComposerContent, actions: CommentComposerActions, material: CommentComposerMaterial,
) {
    val recipe = material.recipe
    val backdrop = material.backdrop
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    // 阴影要落在玻璃自己的裁剪层外面，和底栏、唱片、顶栏圆钮用同一套结构；
    // 否则会被胶囊的 clip 一起裁掉，看起来就像没有阴影。
    Box(
        modifier = Modifier.weight(1f).then(
            if (classic) Modifier else Modifier.teakGlassControlShadow(Capsule()),
        ),
    ) {
        TeakGlassSurface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp, max = 160.dp),
            shape = Capsule(),
            recipe = recipe,
            backdrop = backdrop,
            flatBackgroundColor = if (classic) {
                MiuixTheme.colorScheme.surfaceContainerHigh
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
}
