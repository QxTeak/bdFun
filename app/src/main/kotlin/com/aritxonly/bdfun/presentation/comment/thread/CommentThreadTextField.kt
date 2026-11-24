package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun CommentThreadTextField(
    transient: CommentThreadPresentationStateHolder, target: com.aritxonly.bdfun.core.model.comment.CommentItem,
    focusRequester: FocusRequester,
) {
    val colors = MiuixTheme.colorScheme
    BasicTextField(
        value = transient.draft,
        onValueChange = { transient.draft = it },
        singleLine = false,
        maxLines = 5,
        textStyle = ComposerTextStyle.copy(
            color = colors.onBackground,
        ),
        cursorBrush = SolidColor(BrandPink),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (transient.draft.isEmpty()) {
                    Text(
                        text = "回复 @${target.uname}：",
                        color = colors.outline,
                        style = ComposerTextStyle,
                    )
                }
                inner()
            }
        },
    )
}
