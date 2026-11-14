package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun FeedCard(
    item: FeedItem, actions: FeedCardActions, presentation: FeedCardPresentation,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val shape = ContinuousCornerShape(16.dp)
    val oneTake = rememberOneTakeAnchor()
    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    val photo = LocalPhotoViewer.current
    Column(
        modifier = modifier
            .then(oneTake.modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null) {
                if (presentation.animatedOpen && item.mid > 0L) {
                    oneTake.openFeed(context, item, cardRadiusPx) { actions.onOpen() }
                } else {
                    actions.onOpen()
                }
            }
            .padding(12.dp),
    ) {
        FeedCardAuthor(item)
        FeedCardText(item, presentation.expanded)
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
        FeedCardPgc(item, actions.onOpen)
        FeedCardAttachments(item, actions, presentation) { video ->
            oneTake.open(context, video, cardRadiusPx) { actions.onOpenVideo(video) }
        }
        FeedCardFooter(item, presentation.liked, actions)
    }
}
