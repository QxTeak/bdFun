package com.aritxonly.bdfun

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.message.MessageSession
import com.aritxonly.bdfun.core.navigation.Route
import com.aritxonly.bdfun.feature.message.application.*
import com.aritxonly.bdfun.feature.message.application.MessageFeedKind
import com.aritxonly.bdfun.infra.storage.RouteJsonCodec
import com.aritxonly.bdfun.ui.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.nav.core.NavDisplay
import top.yukonga.miuix.kmp.nav.core.NavDisplayEffects
import top.yukonga.miuix.kmp.nav.core.NavKey
import top.yukonga.miuix.kmp.nav.core.navBackStackOf
import top.yukonga.miuix.kmp.nav.core.rememberNavSystemCornerRadius
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions

@Composable
internal fun MessageCenterHost(onBack: () -> Unit) {
    val owner = AppGraph.session.identity
    key(owner) {
        val navigation = rememberSaveable(saver = messageCenterSaver()) { MessageCenterStateHolder() }
        val entries by navigation.entries.collectAsState()
        val currentBack by rememberUpdatedState(onBack)
        val back = { if (!navigation.back()) currentBack() }
        val master = remember(navigation) {
            movableContentOf { MessageCenterMaster(navigation, entries, currentBack) }
        }
        TeakBareTheme(AppGraph.settings) { _, _ ->
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val width = maxWidth
                val split = usesMessageSplitPane(width.value)
                if (split) {
                    Box(Modifier.width(340.dp).fillMaxHeight()) { master() }
                    Box(Modifier.padding(start = 340.dp).width(1.dp).fillMaxHeight()
                        .background(MiuixTheme.colorScheme.onBackgroundVariant.copy(alpha = .1f)))
                }
                Box(Modifier.align(Alignment.CenterEnd)
                    .width(if (split) width - 341.dp else width).fillMaxHeight()) {
                    MessageCenterPages(entries, navigation, back, split, master)
                }
            }
        }
    }
}

@Composable
private fun MessageCenterMaster(
    state: MessageCenterStateHolder, entries: List<MessageCenterEntry>, onBack: () -> Unit,
) {
    val selected = (entries.firstOrNull()?.destination as? MessageDestination.Page)?.route as? Route.PrivateChat
    val binding = MessageNavigation(
        route = { state.open(MessageDestination.Page(it), fromMaster = true) },
        feed = { kind, tab -> state.open(MessageDestination.Feed(kind, tab), fromMaster = true) },
        back = onBack, selectedSessionId = selected?.let { "${it.sessionType}_${it.talkerId}" },
    )
    CompositionLocalProvider(LocalMessageNavigation provides binding) {
        TeakActivityScaffold(
            settings = AppGraph.settings, onBack = onBack,
            options = ActivityPageOptions(title = "消息", limitContentWidth = false),
            slots = ActivityPageSlots(topBarActions = { MessageTopActions(showContacts = true) }),
        ) { MessageScreenRoute(androidx.compose.ui.platform.LocalContext.current) }
    }
}

private data class MessagePaneRoute(val page: MessageCenterEntry? = null) : NavKey

@Composable
private fun MessageCenterPages(
    entries: List<MessageCenterEntry>,
    state: MessageCenterStateHolder,
    onBack: () -> Unit,
    split: Boolean,
    master: @Composable () -> Unit,
) {
    val binding = MessageNavigation(
        route = { state.open(MessageDestination.Page(it)) },
        feed = { kind, tab -> state.open(MessageDestination.Feed(kind, tab)) },
        back = onBack,
    )
    val desired = listOf(MessagePaneRoute()) + entries.map(::MessagePaneRoute)
    val stack = remember { navBackStackOf(*desired.toTypedArray()) }
    SideEffect { if (stack.toList() != desired) { stack.clear(); stack.addAll(desired) } }
    val systemCornerRadius = rememberNavSystemCornerRadius()
    val effects = remember(split, systemCornerRadius) {
        NavDisplayEffects(enableCornerClip = !split, cornerClipRadius = if (split) 0.dp else systemCornerRadius)
    }
    val motion = if (AppGraph.settings.animationsEnabled) NavTransitions.MiuixDefault else NavTransitions.None
    val transition = remember(motion) {
        // 多级页面被盖住时仍保留私聊草稿、历史列表和设置状态。
        object : NavTransition by motion {
            override val opaqueDepth = Float.MAX_VALUE
        }
    }
    NavDisplay(stack, Modifier.fillMaxSize(), onBack = onBack, transition = transition, effects = effects) {
        entry<MessagePaneRoute>(contentKey = { it.page?.id ?: "message-inbox" }) { route ->
            val page = route.page
            if (page == null) {
                if (split) MessageCenterPlaceholder() else master()
            } else {
                CompositionLocalProvider(LocalMessageNavigation provides binding) {
                    MessageCenterPage(page.destination, onBack)
                }
            }
        }
    }
}

@Composable
private fun MessageCenterPage(destination: MessageDestination, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    when (destination) {
        is MessageDestination.Feed -> MessageDetailScreen(destination.kind, onBack, destination.tab)
        is MessageDestination.Page -> when (val route = destination.route) {
            is Route.MessageTools -> MessageToolsScreen(route, onBack)
            is Route.MessageInbox -> TeakActivityScaffold(
                settings = AppGraph.settings, onBack = onBack,
                options = ActivityPageOptions(title = route.title, limitContentWidth = false),
                slots = ActivityPageSlots(topBarActions = { MessageTopActions() }),
            ) {
                MessageScreen(loggedIn = AppGraph.session.value != null,
                    onNeedLogin = { navigateToLogin(context) }, onBack = onBack,
                    pageType = route.pageType, title = route.title)
            }
            is Route.PrivateChat -> PrivateChatRoot(MessageSession(
                "${route.sessionType}_${route.talkerId}", route.talkerId, route.sessionType,
                route.name, route.avatar, "", 0, route.unread, false, maxSeqno = route.maxSeqno,
            ), onBack)
            else -> Unit
        }
    }
}

@Composable
private fun MessageCenterPlaceholder() {
    Column(Modifier.fillMaxSize().background(MiuixTheme.colorScheme.background),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("选择一条消息", color = MiuixTheme.colorScheme.onBackground)
        Spacer(Modifier.height(12.dp))
        Text("私信、互动与通知，集中查看", color = MiuixTheme.colorScheme.onBackgroundVariant)
    }
}

private fun messageCenterSaver(): Saver<MessageCenterStateHolder, Any> = Saver(
    save = { state -> state.entries.value.map { entry ->
        when (val d = entry.destination) {
            is MessageDestination.Feed -> "feed:${d.kind.name}:${d.tab}"
            is MessageDestination.Page -> RouteJsonCodec().encode(d.route)
        }
    } },
    restore = { saved -> MessageCenterStateHolder((saved as List<*>).mapNotNull { raw ->
        val value = raw as? String ?: return@mapNotNull null
        if (value.startsWith("feed:")) {
            val parts = value.split(':')
            MessageFeedKind.entries.find { it.name == parts.getOrNull(1) }?.let {
                MessageDestination.Feed(it, parts.getOrNull(2)?.toIntOrNull() ?: 0)
            }
        } else RouteJsonCodec().decode(value)?.let(MessageDestination::Page)
    }) },
)
