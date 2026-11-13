package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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

@Composable
internal fun CommentListTitle(bare: Boolean, title: String, total: Int) {
    val colors = MiuixTheme.colorScheme
    if (!bare) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.size(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (total > 0) "评论 $total" else "评论",
                    color = colors.onBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = title,
                    color = colors.onBackgroundVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
internal fun CommentSortRow(total: Int, sortHot: Boolean, onSort: () -> Unit) {
    val colors = MiuixTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "共 ${formatCount(total.toLong())} 条评论",
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = if (sortHot) "按热度" else "按时间",
            color = colors.onBackgroundVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .clip(Capsule())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onSort() }
                .padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
internal fun CommentCardRow(item: CommentItem, disliked: Boolean, actions: CommentListViewActions) {
                        CommentRow(
                            // 不再在这里读全局点赞表，否则点一个赞会让整屏评论行重组。
                            // 每行自己在 CommentRow 里用 derivedStateOf 只订阅自己的值。
                            item = item,
                            disliked = disliked,
                            actions = CommentRowActions(
                                onLike = { actions.onLike(item) },
                                onDislike = { actions.onDislike(item) },
                                onReply = { actions.onReply(item) },
                                onLongPress = { bounds ->
                                    actions.onMenu(item, bounds)
                                },
                                onMore = { bounds ->
                                    actions.onMenu(item, bounds)
                                },
                                onOpenReplies = { actions.onOpenReplies(item, 0L) },
                                // 点折叠回复：不弹回复框，直接展开回复页并定位、闪灰。
                                onReplyToReply = { reply -> actions.onOpenReplies(item, reply.rpid) },
                                onLongPressReply = { reply, bounds ->
                                    actions.onMenu(reply, bounds)
                                },
                                onPreviewLike = { reply -> actions.onLike(reply) },
                            ),
                        )
}

@Composable
internal fun CommentListCard(
    index: Int, lastIndex: Int, item: CommentItem, state: CommentListViewState, actions: CommentListViewActions,
) {
    val colors = MiuixTheme.colorScheme
    val end = state.end
    val highlightRpid = state.highlightRpid
    val disliked = state.disliked
                    val isFirst = index == 0
                    val isLast = index == lastIndex && end
                    val shape = RoundedCornerShape(
                        topStart = if (isFirst) 16.dp else 0.dp,
                        topEnd = if (isFirst) 16.dp else 0.dp,
                        bottomStart = if (isLast) 16.dp else 0.dp,
                        bottomEnd = if (isLast) 16.dp else 0.dp,
                    )
                    val classicComment = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
                    val flashColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (item.rpid == highlightRpid) {
                            colors.onBackground.copy(alpha = 0.10f)
                        } else {
                            colors.surface
                        },
                        animationSpec = androidx.compose.animation.core.tween(90),
                        label = "comment-focus-flash",
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (classicComment) {
                                    Modifier.drawBehind {
                                        val y = size.height - 1.dp.toPx() / 2f
                                        drawLine(
                                            color = colors.onBackground.copy(alpha = 0.10f),
                                            start = androidx.compose.ui.geometry.Offset(0f, y),
                                            end = androidx.compose.ui.geometry.Offset(size.width, y),
                                            strokeWidth = 1.dp.toPx(),
                                        )
                                    }
                                } else {
                                    Modifier.clip(shape).background(flashColor)
                                },
                            ),
                    ) {
                        CommentCardRow(item, disliked[item.rpid] == true, actions)
                    }
}

internal fun androidx.compose.foundation.lazy.LazyListScope.commentListFooter(
    state: CommentListViewState, showLoading: Boolean, textColor: androidx.compose.ui.graphics.Color,
) {
    val loadingMore = state.loadingMore
    val comments = state.comments
    val loading = state.loading
    val error = state.error
    if (loadingMore && showLoading) {
        item(key = "loading-more") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                TeakLoading(size = 36.dp)
            }
        }
    }

    if (comments.isEmpty() && !loading && error == null) {
        item(key = "empty") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有评论",
                    color = textColor,
                    fontSize = 13.sp,
                )
            }
        }
    }

    error?.let { message ->
        item(key = "error") {
            Text(
                text = message,
                modifier = Modifier.padding(vertical = 20.dp, horizontal = 4.dp),
                color = textColor,
                fontSize = 13.sp,
            )
        }
    }
}
