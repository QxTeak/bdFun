package com.aritxonly.bdfun.ui


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.CommentComposerActionRow(
    content: CommentComposerContent, actions: CommentComposerActions,
) {
    val colors = MiuixTheme.colorScheme
    val draft = content.snapshot.draft
    val sending = content.snapshot.sending
    val uploading = content.snapshot.uploading
    val pendingPictures = content.snapshot.pendingPictures
    val pendingPreview = content.pendingPreview
    Row(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .padding(end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pendingPreview != null) {
            AsyncImage(
                model = pendingPreview,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(26.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.surfaceContainerHigh),
            )
            Spacer(Modifier.width(6.dp))
        }
        ComposerIconButton(
            kind = ComposerIcon.Image,
            tint = if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                colors.onBackground.copy(alpha = 0.88f)
            } else colors.onBackgroundVariant,
            onClick = actions.pickImage,
        )
        Spacer(Modifier.width(1.dp))
        ComposerIconButton(
            kind = ComposerIcon.Send,
            tint = if ((draft.isNotBlank() || pendingPictures != null) &&
                !sending && !uploading
            ) {
                BrandPink
            } else {
                if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                    colors.onBackground.copy(alpha = 0.28f)
                } else colors.outline
            },
            onClick = actions::send,
        )
    }
}
