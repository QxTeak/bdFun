package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.aritxonly.bdfun.ui.TeakGlassMenu
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.basic.Text

internal class SpaceToolsUiState {
    var search by mutableStateOf(false)
    var menu by mutableStateOf(false)
    var confirmBlock by mutableStateOf(false)
    var submitted by mutableStateOf(false)
    var dynamic by mutableStateOf(false)
    var anchor by mutableStateOf(Rect.Zero)
}

@Composable
internal fun SpaceToolsUi(runtime: SpaceScreenRuntime, chrome: SpaceChromeState) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val context = LocalContext.current
    val scope = runtime.scope
    TeakGlassMenu(ui.menu, { ui.menu = false }, ui.anchor,
        minWidth = 168.dp, maxWidth = 216.dp, maxHeight = 420.dp) {
        if (!content.isSelf) GlassPopupItem(
            text = if (state.blocked) "已拉黑" else "拉黑",
            enabled = !state.blocked && !state.blockBusy,
            onClick = { ui.menu = false; ui.confirmBlock = true },
        )
        GlassPopupItem(text = "分享", onClick = {
            ui.menu = false
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://space.bilibili.com/${content.mid}")
            }, "分享空间"))
        })
        if (!content.isSelf) GlassPopupItem(text = "举报", onClick = {
            ui.menu = false
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/appeal/?prefid=0&mid=${content.mid}")))
        })
    }
    if (ui.confirmBlock) AlertDialog(onDismissRequest = { ui.confirmBlock = false },
        title = { Text(if (content.loggedIn) "拉黑此用户？" else "请先登录") },
        text = { Text(state.message ?: "拉黑后可在消息设置的黑名单中管理。") },
        confirmButton = { TextButton(enabled = !state.blockBusy, onClick = {
            if (content.loggedIn) scope.launch { controller.block() }
            else ui.confirmBlock = false
        }) { Text(if (state.blockBusy) "处理中…" else "确认") } },
        dismissButton = { TextButton(onClick = { ui.confirmBlock = false }) { Text("关闭") } })
    if (ui.search) SpaceSearchPage(runtime, ui)
}
