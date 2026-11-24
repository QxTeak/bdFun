package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aritxonly.bdfun.ui.ComposerIcon
import com.aritxonly.bdfun.ui.ComposerIconButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 与视频评论区同款：图片预览 + 图片 + 发送三个控件贴在胶囊右侧，
 * 文本区通过 end padding 自动避让，输入前后都是同一个输入框。
 */
@Composable
internal fun androidx.compose.foundation.layout.BoxScope.PrivateChatActionRow(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions,
) {
    val draft = content.draft
    val pendingImage = content.pendingImage
    val sending = content.sending
    val uploading = content.uploading
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pendingImage != null) {
            AsyncImage(
                model = pendingImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.surfaceContainerHigh)
                    .clickable(onClick = actions.onClearImage),
            )
            Spacer(Modifier.width(6.dp))
        }
        ComposerIconButton(
            kind = ComposerIcon.Image,
            tint = colors.onBackgroundVariant,
            onClick = actions.onPickImage,
        )
        Spacer(Modifier.width(1.dp))
        ComposerIconButton(
            kind = ComposerIcon.Send,
            tint = if ((draft.isNotBlank() || pendingImage != null) && !sending && !uploading) {
                colors.primary
            } else {
                colors.outline
            },
            onClick = actions.onSend,
        )
    }
}
