package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import com.aritxonly.bdfun.core.model.comment.CommentItem

/** 仅持有楼中楼窗口的瞬时编辑/菜单/高亮状态，不承担网络与分页。 */
internal class CommentThreadPresentationStateHolder {
    var replyTarget by mutableStateOf<CommentItem?>(null)
    var draft by mutableStateOf("")
    /** 楼中楼按「回复对象」记住草稿（issue #144）：关掉输入框再点同一个人，内容要能带回来。 */
    private val draftMemory = mutableMapOf<Long, String>()
    private var draftKey: Long? = null

    fun bindDraft(key: Long) {
        if (draftKey == key) return
        draftKey?.let { draftMemory[it] = draft }
        draftKey = key
        draft = draftMemory[key].orEmpty()
    }

    /** 退出输入框：把当前草稿挂到对应回复对象名下，再见同一个人时恢复。 */
    fun saveDraft() {
        draftKey?.let { draftMemory[it] = draft }
        draftKey = null
    }

    /** 发送成功：这份草稿已经用掉，不能再被捞回来。 */
    fun forgetDraft(key: Long) {
        draftMemory.remove(key)
        if (draftKey == key) draftKey = null
    }
    /** 楼中楼输入栏的表情面板；和主评论区共用同一份表情面板实现。 */
    var showEmoji by mutableStateOf(false)
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
