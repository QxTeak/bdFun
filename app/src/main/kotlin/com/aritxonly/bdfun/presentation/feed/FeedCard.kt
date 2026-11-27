package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.theme.TeakUiStyle

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedCard(
    item: FeedItem, actions: FeedCardActions, presentation: FeedCardPresentation,
    modifier: Modifier = Modifier, live: Boolean = false,
    /** 详情页：不裁圆角、不铺卡片底色，左右留白直接对齐视频页评论区。 */
    flat: Boolean = false,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val classic = TeakUiStyle.isClassic
    val shape: androidx.compose.ui.graphics.Shape =
        TeakUiStyle.design.cardShape(ContinuousCornerShape(16.dp))
    val oneTake = rememberOneTakeAnchor()
    val cardRadiusPx = if (classic) 0f else with(LocalDensity.current) { 16.dp.toPx() }
    val photo = LocalPhotoViewer.current
    Column(
        modifier = modifier
            .then(oneTake.modifier)
            .fillMaxWidth()
            .then(if (flat) Modifier else Modifier.clip(shape).background(colors.surfaceContainer))
            .feedCardGestures(item, actions, presentation, context, oneTake, cardRadiusPx)
            .padding(horizontal = if (flat) 12.dp else if (classic) 16.dp else 12.dp, vertical = 12.dp),
    ) {
        FeedCardAuthor(
            item,
            live = live,
            moreMorphState = actions.moreMorphState,
            onMore = actions.onMore,
        )
        FeedCardText(item, presentation.expanded)
        FeedCardMedia(item, actions, presentation, photo, oneTake, cardRadiusPx)
        FeedCardFooter(item, presentation.liked, actions)
    }
}

/**
 * 点击 / 长按手势。长按的锚点取自按压点，和首页视频卡片同一套；
 * 滚动期间这里只写普通字段，不进快照状态，避免可见卡片逐帧重组。
 */
@Composable
private fun Modifier.feedCardGestures(
    item: FeedItem,
    actions: FeedCardActions,
    presentation: FeedCardPresentation,
    context: android.content.Context,
    oneTake: OneTakeAnchor,
    cardRadiusPx: Float,
): Modifier {
    val bounds = remember { CardBoundsHolder() }
    val press = remember { CardPressPoint() }
    return this
        .onGloballyPositioned { bounds.attach(it) }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    event.changes.firstOrNull { it.pressed }?.let { press.value = it.position }
                }
            }
        }
        .combinedClickable(
            interactionSource = null,
            indication = null,
            onClick = {
                if (presentation.animatedOpen && item.mid > 0L) {
                    oneTake.openFeed(context, item, cardRadiusPx) { actions.onOpen() }
                } else {
                    actions.onOpen()
                }
            },
            onLongClick = {
                actions.onLongPressRect?.let { open ->
                    val origin = bounds.value
                    val x = origin.left + press.value.x
                    val y = origin.top + press.value.y
                    open(Rect(x, y, x, y))
                }
            },
        )
}

/** 图片 / 番剧卡 / 视频附件：正文与操作行之间的整块内容。 */
@Composable
private fun FeedCardMedia(
    item: FeedItem,
    actions: FeedCardActions,
    presentation: FeedCardPresentation,
    photo: PhotoViewerStateHolder,
    oneTake: OneTakeAnchor,
    cardRadiusPx: Float,
) {
    val context = LocalContext.current
    if (item.images.isNotEmpty()) {
        Spacer(Modifier.height(8.dp))
        FeedImageGrid(images = item.images, onOpen = { url, rect, radius, origins ->
            // 查看器由页面级宿主统一持有：这样关闭动画能作用在整页上（背后页面缩小），
            // 卡片只负责把缩略图矩形和圆角交出去。
            photo.open(
                images = item.images,
                index = item.images.indexOf(url),
                origin = rect,
                originRadiusPx = radius,
                origins = origins,
                footer = { dismiss ->
                    FeedViewerFooter(
                        item = item,
                        liked = presentation.liked,
                        dismissAnimated = dismiss,
                        onLike = actions.onLike,
                        onComment = actions.onComment,
                        onRepost = actions.onRepost,
                    )
                },
            )
        })
    }
    FeedCardPgc(item, actions.onOpen) {
        // 番剧 / 影视卡进 PGC 详情页；拿不到 season_id 时退回原来的动态详情。
        if (item.pgcSeasonId > 0L) {
            // 动态里可能只推了某一集：带上 epId 才不会每次都回退到第一集/续看集。
            com.aritxonly.bdfun.presentation.pgc.openBangumi(
                context, item.pgcSeasonId, item.pgcEpId,
            )
        } else actions.onOpen()
    }
    FeedCardAttachments(item, actions, presentation) { video ->
        oneTake.open(context, video, cardRadiusPx) { actions.onOpenVideo(video) }
    }
}
