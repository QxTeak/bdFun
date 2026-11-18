package com.aritxonly.bdfun.ui


import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import com.aritxonly.bdfun.BrandPink
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
internal fun androidx.compose.foundation.layout.RowScope.HomeSearchEditor(search: HomeTopBarSearch, actions: HomeTopBarActions) {
    val searchResultsVisible = search.resultsVisible
    val searchQuery = search.query
    val onQueryChange = actions.onQueryChange
    val onSubmitSearch = actions.onSubmitSearch
    val focusRequester = remember { FocusRequester() }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(searchResultsVisible) {
        if (searchResultsVisible) {
            // 提交搜索后进入结果页：主动退出输入框并收起输入法。
            focusManager.clearFocus()
            keyboard?.hide()
        } else {
            // 从结果页回到搜索页时，自动聚焦并弹出输入法。
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }
    BasicTextField(
        value = searchQuery,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = ComposerTextStyle.copy(
            color = MiuixTheme.colorScheme.onBackground,
        ),
        cursorBrush = SolidColor(BrandPink),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSubmitSearch() }),
        modifier = Modifier.weight(1f).focusRequester(focusRequester),
        decorationBox = { innerTextField ->
            if (searchQuery.isEmpty()) {
                Text(
                    text = "搜索视频、UP 主",
                    color = MiuixTheme.colorScheme.onBackgroundVariant,
                    style = ComposerTextStyle,
                    maxLines = 1,
                )
            }
            innerTextField()
        },
    )
}
