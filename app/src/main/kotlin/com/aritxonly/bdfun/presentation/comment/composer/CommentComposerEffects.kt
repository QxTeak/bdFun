package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.*
import com.aritxonly.bdfun.feature.comment.application.CommentComposerStateHolder
import kotlinx.coroutines.delay

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
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
        // 点空白处：和返回键同一条路径，输入栏整体退出。
        actions.exit()
    }
    // 按回复对象记忆草稿（issue #144）：同一个视频里给 A 打了一半，转去回 B 再回来回 A，
    // 输入框要把上次对 A 写的内容捞回来。
    val targetName = page.replyTarget.name
    val targetMid = page.replyTarget.mid
    val targetRoot = page.replyTarget.root
    val targetParent = page.replyTarget.parent
    LaunchedEffect(controller, targetName, targetMid, targetRoot, targetParent) {
        if (targetName != null) controller.bindTarget("$targetRoot:$targetParent:$targetMid")
    }
    // 键盘可见时按返回，事件先被输入法吃掉，BackHandler 收不到：所以监听「键盘由显示变隐藏」，
    // 在编辑中时等价于按了返回，直接整体退出输入栏。
    val imeVisible = WindowInsets.isImeVisible
    var wasImeVisible by remember { mutableStateOf(imeVisible) }
    LaunchedEffect(imeVisible, page.replyEditing) {
        if (wasImeVisible && !imeVisible && page.replyEditing) actions.exit()
        wasImeVisible = imeVisible
    }
    // 返回键和「点空白处」完全对齐：清草稿、清图片、清回复对象、收键盘，
    // 不再出现「按返回只是收起、还能继续输入」的半开状态。
    androidx.activity.compose.BackHandler(enabled = page.replyEditing || page.replyTarget.name != null) {
        actions.exit()
    }
    DisposableEffect(controller, page) { onDispose { page.replyEditing = false } }
}
