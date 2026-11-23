package com.aritxonly.bdfun.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.chrome.teakClassicBarGlass
import com.aritxonly.teak.common.material.TeakGlassIconButton
import com.aritxonly.teak.common.material.TeakGlassSurface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.basic.SearchCleanup
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun SettingsCategorySearchButton(onSearch: () -> Unit) {
    // 和页面左上角返回键同尺寸的圆形钮；经典样式由 TeakGlassIconButton 自动退化为平面圆底。
    TeakGlassIconButton(onClick = onSearch, size = 40.dp) {
        Icon(MiuixIcons.Search, "搜索当前分类", tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun BoxScope.SettingsSearchDock(session: SettingsBrowserSession) {
    val searching = session.navigation.searching
    // 经典样式：底部搜索作为贴边栏，整条走 Miuix 玻璃模糊。
    if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) {
        val colors = MiuixTheme.colorScheme
        Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().imePadding().navigationBarsPadding()) {
            Box(Modifier.matchParentSize().teakClassicBarGlass())
            Box(Modifier.matchParentSize().background(colors.surfaceContainer.copy(alpha = 0.62f)))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f).heightIn(min = 44.dp)
                    .then(if (!searching) Modifier.clickable(role = Role.Button) { session.browser.search() } else Modifier)
                    .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(MiuixIcons.Search, null, tint = colors.onSurface, modifier = Modifier.size(20.dp))
                    if (searching) SettingsSearchInput(session)
                    else Text("搜索设置", color = colors.onSurfaceVariantSummary)
                }
            }
        }
        return
    }
    Row(Modifier.align(Alignment.BottomStart).fillMaxWidth()
        .imePadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize(),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        TeakGlassSurface(modifier = Modifier.weight(1f).heightIn(min = 48.dp)
            .then(if (!searching) Modifier.clickable(role = Role.Button) { session.browser.search() } else Modifier)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(MiuixIcons.Search, null, tint = MiuixTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                if (searching) SettingsSearchInput(session)
                else Text("搜索设置", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    if (session.navigation.query.isEmpty()) Text(if (session.navigation.searchRoute == null) "搜索全部设置" else "搜索当前分类",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    input()
                }
                // 与首页搜索框同一个「清空」小图标；退出搜索仍由返回键承担。
                if (session.navigation.query.isNotEmpty()) {
                    Icon(
                        imageVector = MiuixIcons.Basic.SearchCleanup,
                        contentDescription = "清空搜索",
                        tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(role = Role.Button) { session.browser.query("") },
                    )
                }
            }
        })
}
