package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
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
import com.kyant.shapes.Capsule

enum class ComposerIcon { Send, Image }

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
        Image(
            painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(androidx.compose.ui.res.painterResource(
                if (kind == ComposerIcon.Send) TeakIcon.Send else TeakIcon.Image,
            )),
            contentDescription = null,
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(color),
            modifier = Modifier.size(19.dp),
        )
    }
}
