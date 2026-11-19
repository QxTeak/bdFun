package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.basic.Text

internal class SpaceToolsUiState {
    var search by mutableStateOf(false)
    var confirmBlock by mutableStateOf(false)
    var submitted by mutableStateOf(false)
    var dynamic by mutableStateOf(false)
}

/** 与收藏页省略号完全同一套形变菜单：按钮被吸进玻璃，菜单从按钮位置长出来。 */
@Composable
internal fun SpaceToolsMenu(runtime: SpaceScreenRuntime, chrome: SpaceChromeState) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val context = LocalContext.current
    TeakMorphMenuHost(chrome.morphMenu, minWidth = 168.dp, maxWidth = 216.dp, maxHeight = 420.dp) { dismiss ->
        if (!content.isSelf) GlassPopupItem(
            text = if (state.blocked) "已拉黑" else "拉黑",
            enabled = !state.blocked && !state.blockBusy,
            onClick = { dismiss { ui.confirmBlock = true } },
        )
        GlassPopupItem(text = "分享", onClick = {
            dismiss {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://space.bilibili.com/${content.mid}")
                }, "分享空间"))
            }
        })
        if (!content.isSelf) GlassPopupItem(text = "举报", onClick = {
            dismiss {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/appeal/?prefid=0&mid=${content.mid}")))
            }
        })
    }
}

/** 拉黑确认等系统对话框与菜单分开，避免形变菜单宿主被弹窗打断。 */
@Composable
internal fun SpaceToolsDialogs(runtime: SpaceScreenRuntime, chrome: SpaceChromeState) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val scope = runtime.scope
    if (ui.confirmBlock) AlertDialog(onDismissRequest = { ui.confirmBlock = false },
        title = { Text(if (content.loggedIn) "拉黑此用户？" else "请先登录") },
        text = { Text(state.message ?: "拉黑后可在消息设置的黑名单中管理。") },
        confirmButton = { TextButton(enabled = !state.blockBusy, onClick = {
            if (content.loggedIn) scope.launch { controller.block() }
            else ui.confirmBlock = false
        }) { Text(if (state.blockBusy) "处理中…" else "确认") } },
        dismissButton = { TextButton(onClick = { ui.confirmBlock = false }) { Text("关闭") } })
}
