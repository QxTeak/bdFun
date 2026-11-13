package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.model.message.MessageSession
import com.aritxonly.bdfun.ui.TeakActivityScaffold
import com.aritxonly.bdfun.ui.showToast
import kotlinx.coroutines.launch

@Composable
internal fun PrivateChatRoot(session: MessageSession, onBack: (() -> Unit)? = null) {
    val navigation = com.aritxonly.bdfun.ui.LocalMessageNavigation.current
    val context = LocalContext.current
    val runtime = rememberPrivateChatRuntime(session)
    val controller = runtime.controller
    val content by key(controller) { controller.state.collectAsState() }
    val menu = remember(controller) { ChatMessageMenuState() }
    val ui = remember(controller) { PrivateChatPresentationStateHolder() }
    val listState = key(controller) { rememberLazyListState() }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && ui.imagePickerOwner === controller) controller.setImage(uri.toString())
        ui.imagePickerOwner = null
    }
    val image = remember(content.composer.pendingImage) { content.composer.pendingImage?.let(android.net.Uri::parse) }
    TeakActivityScaffold(
        settings = AppGraph.settings,
        onBack = { onBack?.invoke() ?: (context as? android.app.Activity)?.finish() },
        options = ActivityPageOptions(
            title = session.name.ifBlank { session.talkerId.toString() },
            backRevealed = true,
            titleAlwaysVisible = true,
            limitContentWidth = navigation == null,
        ),
        slots = ActivityPageSlots(
            topBarActions = {
                androidx.compose.material3.Text("设置", modifier = Modifier.padding(12.dp).clickable {
                    val route = com.aritxonly.bdfun.core.navigation.Route.MessageTools("chat", session.talkerId.toString())
                    navigation?.route?.invoke(route) ?: context.startActivity(com.aritxonly.bdfun.navigation.createRouteIntent(context, route))
                }, color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.primary)
            },
            overlay = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                PrivateChatComposer(
                    PrivateChatComposerContent(content.composer.draft, content.composer.showEmoji, image,
                        content.composer.sending, content.composer.uploading),
                    PrivateChatComposerActions(
                        controller::setDraft, controller::toggleEmoji, controller::appendEmoji,
                        onPickImage = { ui.imagePickerOwner = controller; imagePicker.launch("image/*") },
                        onClearImage = { controller.setImage(null) },
                        onSend = { runtime.scope.launch {
                            controller.send({ listState.firstVisibleItemIndex == 0 }, { listState.scrollToItem(0) })
                                ?.let { context.showToast(it) }
                        } },
                    ),
                )
            }
        },
        ),
    ) {
        // 富文本与主题依赖由页面壳注入，读取它们的 effect 必须在壳内组合。
        PrivateChatEffects(runtime, ui, session, content, listState)
        com.aritxonly.teak.common.chrome.BindTeakPageScrollState(listState)
        CompositionLocalProvider(LocalChatMessageMenu provides { message -> menu.selected = message; menu.report = false; menu.description = "" },
            LocalChatImageOpen provides { url -> menu.image = url }) {
        PrivateChatMessages(content, session, listState, onRetry = { ui.reloadTick++ },
            onHistory = { runtime.scope.launch { controller.loadHistory() } })
        }
        PrivateChatMessageMenu(menu, controller, runtime.scope)
        ChatImageDialog(menu.image) { menu.image = null }
    }
}
