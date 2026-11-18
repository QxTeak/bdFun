package com.aritxonly.bdfun.ui

import androidx.compose.runtime.*
import com.aritxonly.bdfun.core.model.comment.CommentItem

/** 菜单、展开动画与短暂高亮跟随原评论组合的生命周期。 */
internal class CommentPagePresentationStateHolder {
    var consumedFocusKey by mutableStateOf("")
    var highlightRpid by mutableStateOf(0L)
    /** 高亮序号；同一条评论被再次定位时也要重新闪灰。 */
    var flashSeq by mutableIntStateOf(0)
    var replySheetTarget by mutableStateOf<CommentItem?>(null)
    var replySheetVisible by mutableStateOf(false)
    var replySheetFocusRpid by mutableStateOf(0L)
    val disliked = mutableStateMapOf<Long, Boolean>()
}
