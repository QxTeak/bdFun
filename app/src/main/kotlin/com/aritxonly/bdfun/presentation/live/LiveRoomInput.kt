package com.aritxonly.bdfun.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.BrandPink
import com.aritxonly.teak.common.icons.AppGlyph
import com.aritxonly.teak.common.icons.LucideIcons
import com.kyant.shapes.Capsule

@Composable
internal fun LiveInputBar(
    onSend: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val inputState = remember { LiveInputStateHolder() }
    var text by inputState::text
    val focusManager = LocalFocusManager.current

    // 和视频页输入条一致：底部让出「输入法高度 / 导航栏高度」里较大的那个
    val density = LocalDensity.current
    val imeDp = with(density) { WindowInsets.ime.getBottom(density).toDp().value }
    val navDp = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding().value
    val bottomDp = if (imeDp > navDp) imeDp else navDp

    fun send() {
        val value = text.trim()
        if (value.isEmpty()) return
        onSend(value)
        text = ""
        focusManager.clearFocus()
    }

    Row(
        modifier = modifier
            .padding(bottom = bottomDp.dp, start = 12.dp, end = 12.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(Capsule())

        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(start = 16.dp, end = 46.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = ComposerTextStyle.copy(color = Color.White),
                    cursorBrush = SolidColor(BrandPink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { onFocusChanged(it.isFocused) },
                    decorationBox = { inner -> LiveInputDecoration(text, inner) },
                )
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ComposerIconButton(
                    kind = ComposerIcon.Send,
                    tint = if (text.isNotBlank()) BrandPink else Color.White.copy(alpha = 0.45f),
                    onClick = { send() },
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AppGlyph(LucideIcons.FaceGrinning, size = 18.dp, tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.85f))
        }
    }
}

/* ------------------------------------------------------------------ */
/* 小零件                                                              */
/* ------------------------------------------------------------------ */


@Composable
internal fun LiveMenuRow(label: String, onClick: () -> Unit) {
    val colors = androidx.compose.material3.MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(interactionSource = null, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(text = label, color = colors.onBackground, fontSize = 14.sp)
    }
}


internal tailrec fun Context.findLiveActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findLiveActivity()
    else -> null
}

@Composable
private fun LiveInputDecoration(text: String, inner: @Composable () -> Unit) {
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (text.isEmpty()) {
                                Text(
                                    text = "发一条友善的弹幕",
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 13.sp,
                                )
                            }
                            inner()
                        }
}
