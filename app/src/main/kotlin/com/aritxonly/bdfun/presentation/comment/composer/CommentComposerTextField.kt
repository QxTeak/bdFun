package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentComposerTextField(content: CommentComposerContent, actions: CommentComposerActions) {
    val colors = MiuixTheme.colorScheme
    val draft = content.snapshot.draft
    val pendingPreview = content.pendingPreview
    val focusRequester = content.focusRequester
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = 13.dp, bottom = 13.dp,
                start = 16.dp,
                end = (8f + 64f +
                    (if (pendingPreview != null) 30f else 0f)).dp,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = actions::setDraft,
            singleLine = false,
            maxLines = 5,
            textStyle = ComposerTextStyle.copy(color = colors.onBackground),
            cursorBrush = SolidColor(BrandPink),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { actions.focusChanged(it.isFocused) },

            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) {
                        Text(
                            text = content.replyName?.let { "回复 @$it：" }
                                ?: "发一条友善的评论",
                            color = if (com.aritxonly.teak.common.theme.LocalTeakControlEmphasis.current) {
                                colors.onBackground.copy(alpha = 0.78f)
                            } else colors.onBackgroundVariant,
                            fontSize = 13.sp,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
