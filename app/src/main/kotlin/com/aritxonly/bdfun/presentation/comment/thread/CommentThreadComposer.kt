package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.material.LocalTeakMaterialSpec
import com.aritxonly.teak.common.material.TeakGlassRecipes
import com.aritxonly.teak.common.material.TeakGlassSurface
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun androidx.compose.foundation.layout.BoxScope.CommentThreadComposer(
    transient: CommentThreadPresentationStateHolder, imeVisible: Boolean,
    navBottom: androidx.compose.ui.unit.Dp, focusRequester: FocusRequester, sending: Boolean, sendError: String?, onSend: () -> Unit,
) {
    val target = transient.replyTarget ?: return
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp,
                bottom = if (imeVisible) 8.dp else 8.dp + navBottom)
            .imePadding(),
    ) {
        if (sendError != null) androidx.compose.material3.Text(
            sendError, color = MiuixTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        )
        CommentThreadComposerField(transient, target, focusRequester, sending, onSend)
    }
}

@Composable
private fun CommentThreadComposerField(
    transient: CommentThreadPresentationStateHolder,
    target: com.aritxonly.bdfun.core.model.comment.CommentItem,
    focusRequester: FocusRequester, sending: Boolean, onSend: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    TeakGlassSurface(
        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 160.dp),
        shape = Capsule(),
        recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning),
        backdrop = LocalTeakBackdrop.current,
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = if (sending) 76.dp else 44.dp,
                    top = 12.dp, bottom = 12.dp),
                contentAlignment = Alignment.CenterStart,
            ) { CommentThreadTextField(transient, target, focusRequester) }
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                if (sending) androidx.compose.material3.Text("发送中…")
                else ComposerIconButton(
                    kind = ComposerIcon.Send,
                    tint = if (transient.draft.isNotBlank()) BrandPink else colors.outline,
                    onClick = onSend,
                )
            }
        }
    }
}
