package com.aritxonly.bdfun

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import com.aritxonly.bdfun.feature.space.application.filterSpaceDynamics
import com.aritxonly.bdfun.ui.TeakGlassMenu
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal class SpaceToolsUiState {
    var search by mutableStateOf(false)
    var menu by mutableStateOf(false)
    var confirmBlock by mutableStateOf(false)
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
    TeakGlassMenu(ui.menu, { ui.menu = false }, ui.anchor) {
        if (!content.isSelf) Text(if (state.blocked) "已拉黑" else "拉黑", Modifier.fillMaxWidth()
            .clickable(enabled = !state.blocked && !state.blockBusy) { ui.menu = false; ui.confirmBlock = true }.padding(18.dp))
        Text("分享", Modifier.fillMaxWidth().clickable {
            ui.menu = false
            context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, "https://space.bilibili.com/${content.mid}")
            }, "分享空间"))
        }.padding(18.dp))
        if (!content.isSelf) Text("举报", Modifier.fillMaxWidth().clickable {
            ui.menu = false
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.bilibili.com/appeal/?prefid=0&mid=${content.mid}")))
        }.padding(18.dp))
    }
    if (ui.confirmBlock) AlertDialog(onDismissRequest = { ui.confirmBlock = false },
        title = { Text(if (content.loggedIn) "拉黑此用户？" else "请先登录") },
        text = { Text(state.message ?: "拉黑后可在消息设置的黑名单中管理。") },
        confirmButton = { TextButton(enabled = !state.blockBusy, onClick = {
            if (content.loggedIn) scope.launch { controller.block() }
            else ui.confirmBlock = false
        }) { Text(if (state.blockBusy) "处理中…" else "确认") } },
        dismissButton = { TextButton(onClick = { ui.confirmBlock = false }) { Text("关闭") } })
    SpaceSearchDialog(runtime, ui)
}

@Composable
private fun SpaceSearchDialog(runtime: SpaceScreenRuntime, ui: SpaceToolsUiState) {
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val content = runtime.content
    val context = LocalContext.current
    val scope = runtime.scope
    if (ui.search) Dialog(onDismissRequest = { ui.search = false }) {
        androidx.compose.material3.Surface(color = MiuixTheme.colorScheme.background) {
            Column(Modifier.fillMaxWidth().heightIn(max = 600.dp).padding(16.dp)) {
                Text("搜索此空间")
                BasicTextField(state.query, controller::query, Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = MiuixTheme.colorScheme.onBackground),
                    decorationBox = { input -> if (state.query.isEmpty()) Text("输入关键词"); input() })
                Row {
                    TextButton(onClick = { ui.dynamic = false }) { Text(if (!ui.dynamic) "视频 ✓" else "视频") }
                    TextButton(onClick = { ui.dynamic = true }) { Text(if (ui.dynamic) "动态 ✓" else "动态") }
                    TextButton(onClick = { scope.launch { controller.search() } }, enabled = !ui.dynamic && !state.loading) { Text("搜索") }
                }
                if (ui.dynamic) {
                    val feeds = filterSpaceDynamics(content.snapshot.dynamics.items, state.query)
                    Text("仅搜索已加载动态（${content.snapshot.dynamics.items.size} 条）")
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        if (feeds.isEmpty()) item { Text("没有匹配的动态", Modifier.padding(16.dp)) }
                        items(feeds, key = { it.id }) { feed ->
                            Text(feed.text.ifBlank { feed.video?.title ?: feed.title.ifBlank { "查看动态" } },
                                Modifier.fillMaxWidth().clickable { navigateToFeed(context, feed) }.padding(vertical = 12.dp), maxLines = 3)
                        }
                        item { TextButton(enabled = !content.snapshot.dynamics.loading && !content.snapshot.dynamics.end,
                            onClick = { scope.launch { runtime.controller.loadMoreFeeds() } }) { Text("加载更多动态") } }
                    }
                    content.snapshot.dynamics.error?.let { Text(it) }
                } else {
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        items(state.videos, key = { it.bvid }) { video ->
                            Text(video.title, Modifier.fillMaxWidth().clickable { navigateToVideo(context, video) }.padding(vertical = 12.dp))
                        }
                        if (state.page > 0 && state.videos.isEmpty()) item { Text("没有匹配的视频") }
                        if (state.page > 0 && !state.end) item {
                            TextButton(enabled = !state.loading, onClick = { scope.launch { controller.search() } }) { Text("加载更多") }
                        }
                    }
                    if (state.loading) Text("搜索中…")
                    state.message?.let { Text(it) }
                }
                TextButton(onClick = { ui.search = false }) { Text("关闭") }
            }
        }
    }
}
