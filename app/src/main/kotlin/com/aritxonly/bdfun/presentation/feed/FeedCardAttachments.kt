package com.aritxonly.bdfun.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.core.model.video.VideoItem

@Composable
internal fun FeedCardAttachments(
    item: FeedItem, actions: FeedCardActions, presentation: FeedCardPresentation,
    onOpenVideo: (VideoItem) -> Unit,
) {

    item.liveRecommendation?.let { live ->
        Spacer(Modifier.height(10.dp))
        FeedLiveCard(live)
    }
    item.video?.let { video ->
        Spacer(Modifier.height(10.dp))
        FeedVideoCard(
            video = video,
            isCollection = item.isCollection,
            autoPlay = presentation.autoPlay,
            muted = presentation.muted,
            onOpenVideo = onOpenVideo,
            onToggleMute = actions.onToggleMute,
        )
    }

    item.orig?.let { orig ->
        Spacer(Modifier.height(8.dp))
        ForwardedFeedBlock(
            item = orig,
            onOpen = { actions.onOpenForwarded(orig) },
            onOpenVideo = onOpenVideo,
            autoPlay = presentation.autoPlay,
            muted = presentation.muted,
            onToggleMute = actions.onToggleMute,
        )
    }

}
