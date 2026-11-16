package com.aritxonly.bdfun.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentPreviewLike(liked: Boolean, likeCount: Int, onLike: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = null,
                indication = null,
            ) { onLike() },
    ) {
        Spacer(Modifier.width(2.dp))
        Image(
            painter = com.aritxonly.teak.common.icons.rememberAntialiasedIconPainter(androidx.compose.ui.res.painterResource(
                if (liked) TeakIcon.CommentLikeFilled else TeakIcon.CommentLike,
            )),
            contentDescription = "点赞",
            colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(
                if (liked) BrandPink else colors.onBackgroundVariant,
            ),
            modifier = Modifier.size(12.dp),
        )
        if (likeCount > 0) {
            Spacer(Modifier.width(2.dp))
            Text(
                text = formatCount(likeCount.toLong()),
                color = if (liked) BrandPink else colors.onBackgroundVariant,
                fontSize = 10.sp,
                maxLines = 1,
                modifier = Modifier.offset(y = (-2).dp),
            )
        }
    }
}
