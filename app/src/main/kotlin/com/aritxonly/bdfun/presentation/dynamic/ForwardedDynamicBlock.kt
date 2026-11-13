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
import com.aritxonly.bdfun.core.model.video.VideoItem
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
internal fun ForwardedDynamicBlock(
    item: DynamicItem,
    onOpen: () -> Unit,
    onOpenVideo: (VideoItem) -> Unit,
    autoPlay: Boolean,
    muted: Boolean,
    onToggleMute: () -> Unit,
) {
    val colors = MiuixTheme.colorScheme
    val context = LocalContext.current
    val forwardedOneTake = rememberOneTakeAnchor()
    val forwardedRadiusPx = with(LocalDensity.current) { 12.dp.toPx() }
    val photo = LocalPhotoViewer.current
    Column(
        modifier = Modifier
            .then(forwardedOneTake.modifier)
            .fillMaxWidth()
            .clip(ContinuousCornerShape(12.dp))
            .background(colors.surfaceContainerHigh)
            .clickable(interactionSource = null, indication = null) {
                forwardedOneTake.openDynamic(context, item, forwardedRadiusPx) { onOpen() }
            }
            .padding(10.dp),
    ) {
        ForwardedDynamicAuthor(item)
        ForwardedDynamicText(item)
        if (item.images.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            DynamicImageGrid(
                images = item.images,
                onOpen = { url, rect, radius ->
                    photo.open(
                        images = item.images,
                        index = item.images.indexOf(url),
                        origin = rect,
                        originRadiusPx = radius,
                        footer = { dismiss ->
                            DynamicViewerFooter(item, item.liked, dismiss, {}, onOpen, onOpen)
                        },
                    )
                },
            )
        }
        item.liveRecommendation?.let { live ->
            Spacer(Modifier.height(10.dp))
            DynamicLiveCard(live)
        }
        item.video?.let { video ->
            Spacer(Modifier.height(8.dp))
            DynamicVideoCard(
                video = video,
                isCollection = item.isCollection,
                autoPlay = autoPlay,
                muted = muted,
                onOpenVideo = onOpenVideo,
                onToggleMute = onToggleMute,
            )
        }
    }
}
