package com.aritxonly.bdfun.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.feature.message.application.*
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.teak.common.chrome.TeakLargeTitle
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MessageToolsScreen(route: Route.MessageTools, onBack: () -> Unit) {
    if (route.kind == "contacts" || route.kind == "blacklist") {
        MessageContactsScreen(if (route.kind == "blacklist") 2 else 0, onBack)
        return
    }
    val owner = AppGraph.session.identity
    val tool = remember(route) { when (route.kind) {
        "likes" -> MessageTool.Likes(route.id, route.count)
        "keywords" -> MessageTool.Keywords
        "chat" -> MessageTool.Chat(route.id.toLong())
        else -> MessageTool.Settings(route.type)
    } }
    val controller = remember(owner, tool) { AppGraph.messageToolsStateHolder(tool) }
    val scope = rememberMessageTaskScope(controller, controller::close)
    val state by controller.state.collectAsState()
    val title = when (tool) { is MessageTool.Likes -> "点赞详情"; is MessageTool.Chat -> "聊天设置"; MessageTool.Keywords -> "消息屏蔽词"; else -> state.title.ifBlank { "消息设置" } }
    val list = rememberLazyListState()
    val context = LocalContext.current
    val feed = remember(owner) { AppGraph.messageFeedStateHolder(MessageFeedKind.Like) }
    val feedScope = rememberMessageTaskScope(feed, feed::close)
    LaunchedEffect(controller) { controller.load() }
    MessageResumeEffect(scope) { if (tool is MessageTool.Settings || tool is MessageTool.Chat) controller.load() }
    TeakActivityScaffold(settings = AppGraph.settings, onBack = onBack, options = ActivityPageOptions(title = title, limitContentWidth = LocalMessageNavigation.current == null)) {
        PullToRefreshBox(isRefreshing = state.loading && (state.people.isNotEmpty() || state.settings.isNotEmpty()),
            onRefresh = { scope.launch { controller.load() } }) {
        TrackTeakPageScroll(list)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(
            start = 16.dp, end = 16.dp, top = 8.dp + rememberStatusBarTop(), bottom = 48.dp)) {
            item { TeakLargeTitle(title = title) }
            if (tool is MessageTool.Likes) {
                if (state.title.isNotBlank()) item {
                    Text(state.title, Modifier.fillMaxWidth().clickable {
                        openMessageItem(com.aritxonly.bdfun.core.model.message.MessageItem(route.id, state.title, "", 0, "", "",
                            navigation = route.navigation), MessageFeedKind.Like, feed, feedScope, context)
                    }.padding(16.dp), color = MiuixTheme.colorScheme.primary, fontSize = 15.sp)
                }
                items(state.people, key = { it.mid }) { person ->
                    Row(Modifier.fillMaxWidth().clickable { navigateToSpace(context, person.mid) }.padding(16.dp)) {
                        MessageAvatar(person.avatar)
                        Spacer(Modifier.width(12.dp))
                        Column { Text("${person.name} 赞了我", color = MiuixTheme.colorScheme.primary)
                            Text(formatMessageDate(person.time), color = MiuixTheme.colorScheme.onBackgroundVariant, fontSize = 12.sp) }
                    }
                }
            }
            if (tool is MessageTool.Settings) itemsIndexed(state.settings, key = { _, it -> it.key }) { index, setting ->
                MessageRowSurface(index == 0, index == state.settings.lastIndex) {
                    MessageSettingRow(setting, state.busy || state.loading) { enabled, choice ->
                        scope.launch { controller.setting(setting, enabled, choice)?.let(context::showToast) }
                    }
                }
            }
            if (tool is MessageTool.Chat) item {
                MessageRowSurface(true, true) { MessageChatSettings(tool.mid, state, controller, scope) }
            }
            if (tool == MessageTool.Keywords) item {
                MessageRowSurface(true, true) { MessageKeywordContent(state, controller, scope) }
            }
            item {
                MessagePageFooter(state.loading, state.error, state.more,
                    state.people.isEmpty() && state.settings.isEmpty() && state.keywords == null && state.chat == null) {
                    scope.launch { controller.load(more = if (state.error != null) state.retryMore else state.more && state.people.isNotEmpty()) }
                }
            }
        }
    }
}
}
