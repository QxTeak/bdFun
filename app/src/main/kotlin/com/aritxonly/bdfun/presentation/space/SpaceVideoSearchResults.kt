package com.aritxonly.bdfun

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.ui.SearchResultRow
import com.aritxonly.bdfun.ui.TeakLoading
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.material3.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ColumnScope.SpaceVideoSearchResults(runtime: SpaceScreenRuntime) {
    val controller = runtime.controller.tools
    val state by controller.state.collectAsState()
    val context = LocalContext.current
    val list = rememberLazyGridState()
    val latest by rememberUpdatedState(state)
    LaunchedEffect(list) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { index ->
            val current = latest
            if (current.page > 0 && index >= current.videos.lastIndex - 3 && !current.loading &&
                !current.end && current.message == null) controller.search()
        }
    }
    LaunchedEffect(state.query) { list.scrollToItem(0) }
    LazyVerticalGrid(columns = GridCells.Adaptive(360.dp), state = list, modifier = Modifier.weight(1f),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(state.videos, key = { it.bvid }) { video ->
            SearchResultRow(video, { navigateToVideo(context, video) })
        }
        if (state.loading) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
                TeakLoading(size = 32.dp)
            }
        }
        if (!state.loading && state.videos.isEmpty() && state.message == null) item(span = { GridItemSpan(maxLineSpan) }) {
            Text(if (state.page > 0) "没有找到相关视频" else "输入关键词，搜索这个 UP 的视频",
                color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
        state.message?.let { error -> item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(16.dp)) {
                Text(error, color = MiuixTheme.colorScheme.onBackgroundVariant)
                TextButton(onClick = { runtime.scope.launch { controller.search() } }) { Text("重新搜索") }
            }
        } }
    }
}
