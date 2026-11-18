package com.aritxonly.bdfun

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.feature.space.application.filterSpaceDynamics
import top.yukonga.miuix.kmp.basic.Text

internal class SpaceDynamicSearchUiState {
    var retry by mutableIntStateOf(0)
}

@Composable
internal fun ColumnScope.SpaceDynamicSearchResults(runtime: SpaceScreenRuntime, query: String) {
    val controller = runtime.controller.tools.dynamics
    val state by controller.state.collectAsState()
    val ui = remember(controller) { SpaceDynamicSearchUiState() }
    val context = LocalContext.current
    // 关闭搜索或切回视频会取消遍历；重新打开从已完成的游标续传。
    LaunchedEffect(controller, ui.retry) { controller.loadAll() }
    val feeds = remember(state.items, query) { filterSpaceDynamics(state.items, query) }
    Text(if (state.complete) "全部动态加载完成（${state.items.size} 条）"
        else "${if (state.error != null) "加载中断" else "正在加载全部动态"}：已加载 ${state.pages} 页 / ${state.items.size} 条（结果尚不完整）",
        color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    state.error?.let { Text(it) }
    if (state.error != null) TextButton(onClick = { ui.retry++ }) { Text("重试剩余动态") }
    LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (feeds.isEmpty()) item {
            Text(if (state.complete) "没有匹配的动态" else "暂未找到匹配动态", Modifier.padding(16.dp))
        }
        items(feeds, key = { it.id }) { feed ->
            com.aritxonly.bdfun.ui.FeedCard(
                item = feed,
                actions = com.aritxonly.bdfun.ui.FeedCardActions(
                    onOpen = { navigateToFeed(context, feed) },
                    onOpenVideo = { navigateToVideo(context, it) },
                    onLike = { navigateToFeed(context, feed) },
                    onComment = { navigateToFeed(context, feed) },
                    onRepost = { navigateToFeed(context, feed) }),
                presentation = com.aritxonly.bdfun.ui.FeedCardPresentation(liked = feed.liked),
            )
        }
    }
}
