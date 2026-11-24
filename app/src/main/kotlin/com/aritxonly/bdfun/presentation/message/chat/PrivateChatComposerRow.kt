package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.aritxonly.teak.common.material.teakGlassControlShadow
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 私信输入栏：直接复用视频页评论输入框的同一套结构
 * （玻璃胶囊 + 外阴影 + 46..160dp 高度 + 文字/操作行叠放）。
 */
@Composable
internal fun PrivateChatComposerRow(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions, material: PrivateChatComposerMaterial,
) {
    val onToggleEmoji = actions.onToggleEmoji
    val recipe = material.recipe
    val backdrop = material.backdrop
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    Row(verticalAlignment = Alignment.CenterVertically) {
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
                Box(Modifier.fillMaxWidth()) {
                    PrivateChatTextField(content, actions)
                    PrivateChatActionRow(content, actions)
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        TeakGlassIconButton(
            onClick = onToggleEmoji,
            size = 46.dp,
            recipe = recipe,
            backdrop = backdrop,
        ) {
            AppGlyph(LucideIcons.FaceGrinning, size = 19.dp, tint = MiuixTheme.colorScheme.onBackgroundVariant)
        }
    }
}
