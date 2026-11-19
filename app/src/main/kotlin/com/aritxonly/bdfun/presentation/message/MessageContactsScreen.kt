package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.feature.message.application.MessageTool
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.bdfun.presentation.common.pullrefresh.HomePullRefreshBox
import com.aritxonly.teak.common.chrome.*
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.theme.MiuixTheme

private class ContactPresentationState(initial: Int) {
    var group by mutableIntStateOf(initial)
    var draft by mutableStateOf("")
    var query by mutableStateOf("")
}

@Composable
internal fun MessageContactsScreen(initialGroup: Int, onBack: () -> Unit) {
    val ui = remember { ContactPresentationState(initialGroup) }
    val owner = AppGraph.session.identity
    val controller = remember(owner, ui.group, ui.query) { AppGraph.messageToolsStateHolder(MessageTool.Contacts(ui.group, ui.query)) }
    val scope = rememberMessageTaskScope(controller, controller::close)
    val state by controller.state.collectAsState()
    val list = key(controller) { rememberLazyListState() }
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val navigate = messageRouteAction()
    val title = if (initialGroup == 2) "黑名单" else "联系人"
    LaunchedEffect(controller) { controller.load() }
    MessageResumeEffect(scope) { controller.load() }
    TeakActivityScaffold(settings = AppGraph.settings, onBack = onBack, options = ActivityPageOptions(title = title, titleAlwaysVisible = true, backRevealed = true, limitContentWidth = LocalMessageNavigation.current == null)) {
        HomePullRefreshBox(
            refreshing = state.loading && (state.people.isNotEmpty() || state.settings.isNotEmpty()),
            topPadding = TeakChromeDefaults.TopOverlayHeight - 8.dp,
            canScrollBackward = { list.canScrollBackward },
            isAtTop = { list.firstVisibleItemIndex == 0 && list.firstVisibleItemScrollOffset == 0 },
            scrollToTop = { list.animateScrollToItem(0) },
            onRefresh = { controller.load() },
            modifier = Modifier.fillMaxSize(),
        ) { pullModifier ->
        TrackTeakPageScroll(list)
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().then(pullModifier), contentPadding = PaddingValues(
            start = TeakUiStyle.design.contentInset(16.dp),
            end = TeakUiStyle.design.contentInset(16.dp),
            top = TeakChromeDefaults.TopOverlayHeight + rememberStatusBarTop(), bottom = 40.dp)) {
            if (initialGroup != 2) item {
                com.aritxonly.teak.common.navigation.TeakPagerTabs(
                    options = listOf("我的关注", "我的粉丝"), selected = ui.group,
                    onSelect = { ui.group = it; ui.query = ""; ui.draft = "" },
                    itemWidth = 96.dp, backdrop = null, plain = true,
                )
                if (ui.group == 0) Row {
                    BasicTextField(ui.draft, { ui.draft = it }, modifier = Modifier.weight(1f).padding(16.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(color = colors.onBackground),
                        decorationBox = { input -> if (ui.draft.isEmpty()) Text("搜索关注的人", color = colors.onBackgroundVariant); input() })
                    Text("搜索", color = colors.primary, modifier = Modifier.clickable { ui.query = ui.draft.trim() }.padding(16.dp))
                }
            }
            items(state.people, key = { it.mid }) { person ->
                Row(Modifier.fillMaxWidth().clickable {
                    if (initialGroup == 2) navigateToSpace(context, person.mid)
                    else navigate(com.aritxonly.bdfun.core.navigation.Route.PrivateChat(
                        talkerId = person.mid, name = person.name, avatar = person.avatar))
                }.padding(16.dp)) {
                    MessageAvatar(person.avatar); Spacer(Modifier.width(12.dp))
                    Text(person.name, color = colors.onBackground, modifier = Modifier.weight(1f))
                    if (initialGroup == 2) Text("移除", color = colors.primary, modifier = Modifier.clickable(enabled = !state.busy) {
                        scope.launch { controller.chatPreference(person.mid, com.aritxonly.bdfun.core.domain.ChatPreferenceAction.Block, false)?.let(context::showToast) }
                    })
                }
            }
            item { MessagePageFooter(state.loading, state.error, state.more, state.people.isEmpty()) {
                scope.launch { controller.load(if (state.error != null) state.retryMore else state.more) }
            } }
        }
    }
}
}
