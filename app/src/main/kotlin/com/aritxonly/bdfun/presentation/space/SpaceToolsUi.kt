package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.aritxonly.teak.common.modal.AlertDialog
import com.aritxonly.teak.common.modal.TextButton
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.basic.Text as MiuixText

internal class SpaceToolsUiState {
    var search by mutableStateOf(false)
    var confirmBlock by mutableStateOf(false)
    var submitted by mutableStateOf(false)
    var dynamic by mutableStateOf(false)
}

private val SpaceDangerRed = Color(0xFFE5484D)

/** 与收藏页省略号完全同一套形变菜单：按钮被吸进玻璃，菜单从按钮位置长出来。 */
@Composable
internal fun SpaceToolsMenu(runtime: SpaceScreenRuntime, chrome: SpaceChromeState) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val context = LocalContext.current
    TeakMorphMenuHost(chrome.morphMenu, minWidth = 168.dp, maxWidth = 216.dp, maxHeight = 420.dp) { dismiss ->
        // 分享放最上面；拉黑/举报是危险操作，统一红色。
        GlassPopupItem(text = "分享", onClick = {
            dismiss {
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "https://space.bilibili.com/${content.mid}")
                }, "分享空间"))
            }
        })
        if (!content.isSelf) GlassPopupItem(
            text = if (state.blocked) "已拉黑" else "拉黑",
            enabled = !state.blocked && !state.blockBusy,
            contentColor = SpaceDangerRed,
            onClick = { dismiss { ui.confirmBlock = true } },
        )
        if (!content.isSelf) GlassPopupItem(
            text = "举报",
            contentColor = SpaceDangerRed,
            onClick = {
                dismiss {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/appeal/?prefid=0&mid=${content.mid}")))
                }
            },
        )
    }
}

/** 拉黑确认等弹窗与菜单分开，避免形变菜单宿主被弹窗打断。 */
@Composable
internal fun SpaceToolsDialogs(
    runtime: SpaceScreenRuntime,
    chrome: SpaceChromeState,
    onBlocked: () -> Unit = {},
) {
    val ui = chrome.tools
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val scope = runtime.scope
    AlertDialog(
        show = ui.confirmBlock,
        onDismissRequest = { if (!state.blockBusy) ui.confirmBlock = false },
        miuixTitle = if (content.loggedIn) "拉黑此用户？" else "请先登录",
        miuixSummary = state.message ?: "拉黑后可在消息设置的黑名单中管理。",
        confirmButton = {
            TextButton(
                enabled = !state.blockBusy,
                miuixText = if (state.blockBusy) "处理中…" else "拉黑",
                destructive = true,
                onClick = {
                    if (!content.loggedIn) {
                        ui.confirmBlock = false
                    } else {
                        scope.launch {
                            // 拉黑成功立即退出该用户主页，避免继续展示已拉黑关系。
                            if (controller.block()) {
                                ui.confirmBlock = false
                                onBlocked()
                            }
                        }
                    }
                },
            ) { Text(if (state.blockBusy) "处理中…" else "拉黑") }
        },
        dismissButton = {
            TextButton(enabled = !state.blockBusy, miuixText = "取消", onClick = { ui.confirmBlock = false }) {
                MiuixText("取消")
            }
        },
    )
}
