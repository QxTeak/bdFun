package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.model.comment.CommentItem

/** 仅持有楼中楼窗口的瞬时编辑/菜单/高亮状态，不承担网络与分页。 */
internal class CommentThreadPresentationStateHolder {
    var replyTarget by mutableStateOf<CommentItem?>(null)
    var draft by mutableStateOf("")
    var flashRpid by mutableLongStateOf(0L)
    /** 高亮序号；同一条回复被再次定位/发送时也要重新闪灰。 */
    var flashSeq by mutableIntStateOf(0)
    val dislikedIds = mutableStateMapOf<Long, Boolean>()
    var menuReply by mutableStateOf<CommentItem?>(null)
    var menuVisible by mutableStateOf(false)
    var menuAnchor by mutableStateOf(Rect.Zero)
    var consumedFocusRpid by mutableLongStateOf(0L)
}

internal class CommentThreadMountStateHolder { var mounted by mutableStateOf(false) }
