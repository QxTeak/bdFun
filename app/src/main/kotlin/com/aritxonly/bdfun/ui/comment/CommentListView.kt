package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.data.CommentItem
import com.kyant.shapes.Capsule
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.getValue

internal data class CommentListViewState(
    val comments: List<CommentItem>, val total: Int, val loading: Boolean, val loadingMore: Boolean,
    val end: Boolean, val error: String?, val sortHot: Boolean, val highlightRpid: Long,
    val disliked: Map<Long, Boolean>,
    /** 每次定位/发送成功都会 +1，让同一条评论能重复闪灰。 */
    val flashSeq: Int = 0,
)
internal data class CommentListViewLayout(
    val bare: Boolean, val title: String, val topInset: androidx.compose.ui.unit.Dp,
    val bottomPadding: androidx.compose.ui.unit.Dp, val showLoading: Boolean,
    val header: (androidx.compose.foundation.lazy.LazyListScope.() -> Unit)?,
    val flatList: Boolean = false,
    /** 视频详情竖屏主评论区用更紧的标题上下留白。 */
    val compactHeaderSpacing: Boolean = false,
)
internal class CommentListViewActions(
    val onSort: () -> Unit, val onLike: (CommentItem) -> Unit, val onDislike: (CommentItem) -> Unit,
    val onReply: (CommentItem) -> Unit, val onMenu: (CommentItem, androidx.compose.ui.geometry.Rect) -> Unit,
    val onOpenReplies: (CommentItem, Long) -> Unit,
)

@Composable
internal fun CommentListView(
    state: CommentListViewState, layout: CommentListViewLayout, actions: CommentListViewActions,
    listState: androidx.compose.foundation.lazy.LazyListState,
) {
    val colors = MiuixTheme.colorScheme
    val comments = state.comments
    val total = state.total
    val loading = state.loading
    val sortHot = state.sortHot
    val bare = layout.bare
    val title = layout.title
    val topInset = layout.topInset
    val bottomPadding = layout.bottomPadding
    val showLoading = layout.showLoading
    val header = layout.header
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
    ) {
        CommentListTitle(bare, title, total)

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier,
                contentPadding = PaddingValues(
                    start = if (layout.flatList) {
                        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 10.dp else 4.dp
                    } else if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 12.dp,
                    end = if (layout.flatList) {
                        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 10.dp else 4.dp
                    } else if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 12.dp,
                    top = 4.dp + topInset,
                    bottom = bottomPadding,
                ),
            ) {
                header?.invoke(this)
                item(key = "comment-count") {
                    Box(Modifier.padding(horizontal = if (layout.flatList) {
                        if (com.aritxonly.teak.common.theme.TeakUiStyle.isClassic) 2.dp else 8.dp
                    } else 0.dp)) {
                        CommentSortRow(total, sortHot, actions.onSort, layout.compactHeaderSpacing)
                    }
                }

                // 视频详情平铺评论，其余入口保留原分组样式。
                if (loading && comments.isEmpty() && showLoading) {
                    commentSkeletonItems()
                }
                itemsIndexed(comments, key = { _, item -> item.rpid }) { index, item ->
                    CommentListCard(index, comments.lastIndex, item, state, actions, layout.flatList)
                }

                commentListFooter(state, showLoading, colors.onBackgroundVariant)
            }

        }
    }

}
