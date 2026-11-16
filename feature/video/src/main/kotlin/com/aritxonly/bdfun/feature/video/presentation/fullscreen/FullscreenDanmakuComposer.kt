package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.model.video.DanmakuItem
import com.aritxonly.bdfun.feature.video.application.FullscreenDanmakuComposerStateHolder
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule
import kotlinx.coroutines.delay

@Composable
fun FullscreenDanmakuInput(
    onDismiss: () -> Unit,
    items: List<DanmakuItem> = emptyList(),
    modifier: Modifier = Modifier,
    sender: () -> (suspend (String, Int, Int, Int) -> Boolean)?,
) {

    val ui = remember { FullscreenDanmakuComposerStateHolder() }
    DisposableEffect(ui) { onDispose { ui.close() } }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()


    LaunchedEffect(Unit) {
        delay(80)
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }

    fun submit() {
        ui.submit(sender(), scope, onDismiss) {
            focusManager.clearFocus()
            keyboard?.hide()
            onDismiss()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF20A0A0C))
            .imePadding()
            .padding(horizontal = 26.dp, vertical = 10.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        ComposerInputRow(ui, focusRequester, ::submit)
        Spacer(Modifier.height(14.dp))
        ComposerInputSettings(ui)
    }
}

private val ComposerColors = listOf(
        0xFFFFFF, 0xFB7299, 0xFF4D4D, 0xFFD93B, 0x36C46A,
        0x2F9BFF, 0xFF4FA8, 0x9A6BFF, 0x6ED6E8, 0xFFA33B,
    )

@Composable
private fun ComposerInputRow(
    ui: FullscreenDanmakuComposerStateHolder, focusRequester: FocusRequester, onSubmit: () -> Unit,
) {
    val state by ui.state.collectAsState()
    val text = state.text
    val sending = state.sending
    val fontSize = state.fontSize
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(interactionSource = null, indication = null) {
                    ui.setFontSize(if (fontSize == 25) 18 else 25)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "A", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(8.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(Capsule())
                .background(Color.White.copy(alpha = 0.94f))
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ComposerTextField(ui, focusRequester, onSubmit, Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = text.isNotBlank() && !sending,
                        onClick = { onSubmit() },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AppGlyph(LucideIcons.SendHorizontalFilled, size = 20.dp, tint = if (text.isBlank()) Color.Black.copy(alpha = 0.25f) else Color(0xFFFB7299))
            }
        }
    }

}

@Composable
private fun ComposerTextField(
    ui: FullscreenDanmakuComposerStateHolder, focusRequester: FocusRequester,
    onSubmit: () -> Unit, modifier: Modifier,
) {
    val state by ui.state.collectAsState()
    val text = state.text
    BasicTextField(
        value = text,
        onValueChange = ui::edit,
        singleLine = true,
        textStyle = TextStyle(
            color = Color.Black,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
        ),
        cursorBrush = SolidColor(Color(0xFFFB7299)),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = { onSubmit() }),
        modifier = modifier
            .focusRequester(focusRequester),
        decorationBox = { inner ->
            Box(contentAlignment = Alignment.CenterStart) {
                if (text.isEmpty()) {
                    Text(
                        text = "发个友善的弹幕见证当下",
                        color = Color.Black.copy(alpha = 0.4f),
                        fontSize = 13.sp,
                    )
                }
                inner()
            }
        },
    )
}

@Composable
private fun ComposerInputSettings(ui: FullscreenDanmakuComposerStateHolder) {
    val state by ui.state.collectAsState()
    val fontSize = state.fontSize
    val mode = state.mode
    val color = state.color
    ComposerSettingRow(label = "字体大小") {
        ComposerChip("默认", fontSize == 25) { ui.setFontSize(25) }
        Spacer(Modifier.width(10.dp))
        ComposerChip("较小", fontSize == 18) { ui.setFontSize(18) }
    }
    ComposerSettingRow(label = "弹幕位置") {
        ComposerChip("滚动", mode == 1) { ui.setMode(1) }
        Spacer(Modifier.width(10.dp))
        ComposerChip("置顶", mode == 5) { ui.setMode(5) }
        Spacer(Modifier.width(10.dp))
        ComposerChip("置底", mode == 4) { ui.setMode(4) }
    }
    ComposerSettingRow(label = "弹幕颜色") {
        Row(
            modifier = Modifier.padding(start = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            ComposerColors.forEach { c ->
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF000000.toInt() or c))
                        .border(
                            if (color == c) 2.dp else 0.5.dp,
                            if (color == c) Color.White else Color.White.copy(alpha = 0.35f),
                            CircleShape,
                        )
                        .clickable(interactionSource = null, indication = null) { ui.setColor(c) },
                )
            }
        }
    }
}

@Composable
internal fun ComposerSettingRow(label: String, content: @Composable () -> Unit) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp,
            modifier = Modifier.width(78.dp),
        )
        content()
    }
}

@Composable
internal fun ComposerChip(text: String, selected: Boolean, onClick: () -> Unit) {

    val pink = Color(0xFFFB7299)
    Text(
        text = text,
        color = if (selected) pink else Color.White.copy(alpha = 0.8f),
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .width(64.dp)
            .clip(Capsule())
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
    )
}
