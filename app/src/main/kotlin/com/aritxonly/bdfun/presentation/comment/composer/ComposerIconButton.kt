package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.icons.AppGlyph
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule

enum class ComposerIcon { Send, Image, Emoji }

/** 输入框内的两个小按钮：尺寸完全一致，按压时统一变粉。 */
@Composable
fun ComposerIconButton(
    kind: ComposerIcon,
    tint: Color,
    onClick: () -> Unit,
) {

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val color = if (pressed) BrandPink else tint
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(Capsule())
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when (kind) {
            ComposerIcon.Send -> AppGlyph(TeakIcon.Send, color, size = 19.dp)
            ComposerIcon.Image -> AppGlyph(MiuixIcons.Image, color, size = 19.dp)
            // 楼中楼输入栏的表情入口：和主评论区用同一个笑脸图标。
            ComposerIcon.Emoji -> AppGlyph(LucideIcons.FaceGrinning, color, size = 19.dp)
        }
    }
}
