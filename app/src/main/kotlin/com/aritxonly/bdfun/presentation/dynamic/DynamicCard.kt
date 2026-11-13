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
import com.aritxonly.bdfun.core.model.feed.DynamicItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun DynamicCard(
    item: DynamicItem, actions: DynamicCardActions, presentation: DynamicCardPresentation,
    modifier: Modifier = Modifier,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val shape = ContinuousCornerShape(16.dp)
    val oneTake = rememberOneTakeAnchor()
    val cardRadiusPx = with(LocalDensity.current) { 16.dp.toPx() }
    val viewer = remember { DynamicImageSelectionStateHolder() }
    Column(
        modifier = modifier
            .then(oneTake.modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .clickable(interactionSource = null, indication = null) {
                if (presentation.animatedOpen && item.mid > 0L) {
                    oneTake.openDynamic(context, item, cardRadiusPx) { actions.onOpen() }
                } else {
                    actions.onOpen()
                }
            }
            .padding(12.dp),
    ) {
        DynamicCardAuthor(item)
        DynamicCardText(item, presentation.expanded)
        if (item.images.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            DynamicImageGrid(images = item.images, onOpen = { url, rect ->
                viewer.viewerUrl = url; viewer.viewerOrigin = rect
            })
        }
        DynamicCardPgc(item, actions.onOpen)
        DynamicCardAttachments(item, actions, presentation) { video ->
            oneTake.open(context, video, cardRadiusPx) { actions.onOpenVideo(video) }
        }
        DynamicCardFooter(item, presentation.liked, actions)
    }
    viewer.viewerUrl?.let { url ->
        DynamicImageViewer(item, url, viewer.viewerOrigin, presentation.liked,
            onDismiss = { viewer.viewerUrl = null }, onLike = actions.onLike,
            onComment = actions.onComment, onRepost = actions.onRepost)
    }
}
