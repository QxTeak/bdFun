package com.aritxonly.bdfun.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.comment.CommentItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun CommentRow(item: CommentItem, disliked: Boolean, actions: CommentRowActions) {
    val likes = LocalVideoPageState.current.dependencies.comments.likes
    val ui = remember { CommentRowPresentationStateHolder() }
    // 只订阅这条评论自己的点赞状态；别的行被点赞时这里不会失效。
    val liked by remember(item.rpid, item.liked) {
        derivedStateOf { likes.likedMap[item.rpid] ?: item.liked }
    }
    val like by remember(item.rpid, item.like) {
        derivedStateOf { likes.countMap[item.rpid] ?: item.like }
    }
    // combinedClickable 的 onLongClick 不给坐标，这里单独记一下按下的位置，
    // 菜单就会弹在手指按下的地方，而不是固定贴在这一行的省略号上。
    var pressPosition by remember { mutableStateOf(Offset.Zero) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                ui.bounds = it.boundsInWindow()
            }
            // 只旁听指针位置，不消费任何事件：长按由 combinedClickable 判定，
            // 我们只是把「按下的坐标」记下来，好让菜单弹在手指处。
            .pointerInput(item.rpid) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.pressed } ?: continue
                        pressPosition = change.position
                    }
                }
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = actions.onReply,
                onLongClick = {
                    actions.onLongPress(
                        Rect(
                            left = ui.bounds.left + pressPosition.x,
                            top = ui.bounds.top + pressPosition.y,
                            right = ui.bounds.left + pressPosition.x,
                            bottom = ui.bounds.top + pressPosition.y,
                        ),
                    )
                },
            )
            .padding(
                horizontal = if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 6.dp else 12.dp,
                vertical = 10.dp,
            ),
    ) {
        CommentRowBody(item, disliked, liked, like, ui, actions)
    }
}
