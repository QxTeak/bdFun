package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentRowDislike(disliked: Boolean, onDislike: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .offset(y = 1.5.dp)
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onDislike,
            ),
    ) {
        Image(
            painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(androidx.compose.ui.res.painterResource(
                if (disliked) TeakIcon.CommentDislikeFilled else TeakIcon.CommentDislike,
            )),
            contentDescription = "点踩",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                if (disliked) BrandPink else colors.onBackgroundVariant,
            ),
            modifier = Modifier.size(16.dp),
        )
    }
}
