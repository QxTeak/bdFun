package com.aritxonly.bdfun.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Close
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsCategorySearchButton(onSearch: () -> Unit) {
    TeakGlassIconButton(onClick = onSearch) {
        Icon(MiuixIcons.Search, "搜索当前分类", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun BoxScope.SettingsSearchDock(session: SettingsBrowserSession) {
    val searching = session.navigation.searching
    val start = if (session.wide && searching) 280.dp else 0.dp
    val width = if (session.wide && !searching) Modifier.width(280.dp) else Modifier.fillMaxWidth()
    Row(Modifier.align(Alignment.BottomStart).then(width).padding(start = start)
        .imePadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 16.dp).animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        TeakGlassSurface(modifier = Modifier.weight(1f).heightIn(min = 54.dp)
            .then(if (!searching) Modifier.clickable(role = Role.Button) { session.browser.search() } else Modifier)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(MiuixIcons.Search, null, tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                if (searching) SettingsSearchInput(session)
                else Text("搜索设置", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
            }
        }
        AnimatedVisibility(searching, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
            TeakGlassIconButton(onClick = session.browser::closeSearch, size = 54.dp) {
                Icon(MiuixIcons.Close, "关闭搜索", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun SettingsSearchInput(session: SettingsBrowserSession) {
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { requester.requestFocus(); keyboard?.show() }
    BasicTextField(value = session.navigation.query, onValueChange = session.browser::query,
        modifier = Modifier.fillMaxWidth().focusRequester(requester), singleLine = true,
        textStyle = TextStyle(color = MiuixTheme.colorScheme.onSurface, fontSize = 16.sp),
        cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        decorationBox = { input ->
            Box {
                if (session.navigation.query.isEmpty()) Text(if (session.navigation.searchRoute == null) "搜索全部设置" else "搜索当前分类",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                input()
            }
        })
}
