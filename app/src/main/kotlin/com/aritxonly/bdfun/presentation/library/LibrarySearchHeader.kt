package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.teak.common.icons.AppGlyph
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.*
import top.yukonga.miuix.kmp.icon.extended.*
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun LibrarySearchHeader(title: String, state: LibrarySearchState, events: LibrarySearchEvents, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, top = 8.dp, end = 12.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibrarySearchBackButton(onBack)
        Spacer(Modifier.width(10.dp))
        LibrarySearchField(title, state, events)
    }
}

@Composable
internal fun LibrarySearchBackButton(onBack: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null, onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        AppGlyph(MiuixIcons.ChevronBackward, colors.onBackground, size = 18.dp)
    }
}

@Composable
internal fun androidx.compose.foundation.layout.RowScope.LibrarySearchField(title: String, state: LibrarySearchState, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clip(Capsule())
            .background(colors.surfaceContainerHigh)
            .padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LibrarySearchMagnifier()
        Spacer(Modifier.width(8.dp))
        LibrarySearchInput(title, state, events)
        LibrarySearchClearButton(state, events)
    }
}

@Composable
internal fun LibrarySearchMagnifier() {
    val colors = MiuixTheme.colorScheme
    AppGlyph(MiuixIcons.Search, colors.onBackgroundVariant, size = 16.dp)
}

@Composable
internal fun androidx.compose.foundation.layout.RowScope.LibrarySearchInput(title: String, state: LibrarySearchState, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
        if (state.draft.isEmpty()) {
            Text(
                text = "搜索$title",
                color = colors.onBackgroundVariant,
                fontSize = 13.5.sp,
            )
        }
        BasicTextField(
            value = state.draft,
            onValueChange = events::changeDraft,
            singleLine = true,
            textStyle = TextStyle(color = colors.onBackground, fontSize = 13.5.sp),
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                events.submit()
            }),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
internal fun LibrarySearchClearButton(state: LibrarySearchState, events: LibrarySearchEvents) {
    val colors = MiuixTheme.colorScheme
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable(interactionSource = null, indication = null) {
                events.clearInput()
            },
        contentAlignment = Alignment.Center,
    ) {
        if (state.draft.isNotEmpty()) {
            AppGlyph(MiuixIcons.Basic.SearchCleanup, colors.onBackgroundVariant, size = 14.dp)
        }
    }
}
