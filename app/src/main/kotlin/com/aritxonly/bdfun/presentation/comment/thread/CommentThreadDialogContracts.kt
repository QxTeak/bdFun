package com.aritxonly.bdfun.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import com.aritxonly.bdfun.core.model.comment.CommentItem

internal data class ThreadSheetContent(
    val comment: CommentItem, val allReplies: List<CommentItem>, val extraCount: Int,
    val loading: Boolean, val end: Boolean, val sortHot: Boolean, val sending: Boolean,
)
internal data class ThreadSheetLayout(
    val progress: Float, val minTopPx: Float, val playerBottomDp: androidx.compose.ui.unit.Dp?,
    val paneWidth: androidx.compose.ui.unit.Dp?, val imeVisible: Boolean, val navBottom: androidx.compose.ui.unit.Dp,
    /** 主评论区是否是「视频详情平铺」布局；展开页左右留白要跟着它走。 */
    val flatList: Boolean = false,
)
internal class ThreadSheetCallbacks(
    val onDismiss: () -> Unit, val onToggleSort: () -> Unit, val onExitReply: () -> Unit,
    val onSend: () -> Unit, val onLike: (CommentItem) -> Unit,
)

internal data class CommentThreadRender(
    val content: ThreadSheetContent, val layout: ThreadSheetLayout,
    val transient: CommentThreadPresentationStateHolder,
    val listState: androidx.compose.foundation.lazy.LazyListState,
    val connection: androidx.compose.ui.input.nestedscroll.NestedScrollConnection,
    val focusRequester: FocusRequester, val callbacks: ThreadSheetCallbacks,
)
