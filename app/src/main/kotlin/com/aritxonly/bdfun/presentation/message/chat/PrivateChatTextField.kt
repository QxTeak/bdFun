package com.aritxonly.bdfun

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.ComposerTextStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun PrivateChatTextField(
    content: PrivateChatComposerContent, actions: PrivateChatComposerActions,
) {
    val draft = content.draft
    val onDraftChange = actions.onDraftChange
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 13.dp, bottom = 13.dp, start = 16.dp, end = 72.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            maxLines = 5,
            textStyle = ComposerTextStyle.copy(color = colors.onBackground),
            cursorBrush = SolidColor(colors.primary),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (draft.isEmpty()) {
                        Text(
                            text = "发个消息聊聊呗",
                            color = colors.onBackgroundVariant,
                            style = ComposerTextStyle,
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
