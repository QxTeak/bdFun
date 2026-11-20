package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.feature.comment.application.CommentComposerStateHolder
import kotlinx.coroutines.delay

@Composable
internal fun CommentComposerEffects(
    page: VideoPagePresentationState, controller: CommentComposerStateHolder, actions: CommentComposerActions,
) {
    // 先消费请求，延迟聚焦期间不要改动 key；保留原 60ms 和重复请求幂等规则。
    LaunchedEffect(controller, page.requestReplyFocus) {
        val tick = page.requestReplyFocus
        if (tick == 0 || tick == page.consumedReplyFocus) return@LaunchedEffect
        page.consumedReplyFocus = tick
        delay(60)
        actions.focusReply()
    }
    LaunchedEffect(controller, page.dismissReply) {
        val tick = page.dismissReply
        if (tick == 0 || tick == page.consumedDismissReply) return@LaunchedEffect
        page.consumedDismissReply = tick
        actions.dismiss()
    }
    // 返回键和「点空白处」走同一条收起路径：清焦点、收键盘、退出编辑态，
    // 草稿为空时连回复目标一起清掉。以前这里没人接返回，按一下只是把键盘收下去，
    // 输入框还停在编辑态，还能继续打字。
    androidx.activity.compose.BackHandler(enabled = page.replyEditing) { actions.dismiss() }
    DisposableEffect(controller, page) { onDispose { page.replyEditing = false } }
}
