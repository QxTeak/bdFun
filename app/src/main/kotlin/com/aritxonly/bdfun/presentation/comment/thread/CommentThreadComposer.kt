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
import com.aritxonly.teak.common.material.teakGlassControlShadow
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
        // 展开楼中楼时输入栏仍留在最上面一层，表情面板直接挂在这里，
        // 不再需要回到主评论区的输入栏才能发表情（issue #144）。
        if (transient.showEmoji) {
            EmojiPanel(
                onPick = { transient.draft += it },
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
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
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    // 楼中楼输入框和主评论区输入框共用同一套玻璃 + 外置阴影，避免这里看起来是平贴的。
    Box(
        modifier = Modifier.fillMaxWidth().then(
            if (classic) Modifier else Modifier.teakGlassControlShadow(Capsule()),
        ),
    ) {
        TeakGlassSurface(
            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp, max = 160.dp),
            shape = Capsule(),
            recipe = TeakGlassRecipes.floatingNavigation(LocalTeakMaterialSpec.current.fineTuning),
            backdrop = LocalTeakBackdrop.current,
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth()) {
                Box(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = if (sending) 76.dp else 80.dp,
                        top = 12.dp, bottom = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) { CommentThreadTextField(transient, target, focusRequester) }
                Box(Modifier.align(Alignment.CenterEnd).padding(end = 40.dp)) {
                    ComposerIconButton(
                        kind = ComposerIcon.Emoji,
                        tint = if (transient.showEmoji) BrandPink else colors.outline,
                        onClick = { transient.showEmoji = !transient.showEmoji },
                    )
                }
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
}
