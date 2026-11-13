package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToFeed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.ui.FeedCard
import com.aritxonly.bdfun.ui.TeakLoading

internal fun androidx.compose.foundation.lazy.LazyListScope.spaceFeedItems(
    content: SpaceScreenContent, actions: SpaceScreenActions,
) {
    val context = actions.context
    val dynamics = content.snapshot.dynamics.items
    val dynamicLoading = content.snapshot.dynamics.loading
    val loading = content.snapshot.profile.loading || !content.banner.ready
    val tabEnterModifier = Modifier
    if (dynamics.isEmpty() && loading) {
        item(key = "dynamic-loading") {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                TeakLoading(size = 34.dp)
            }
        }
    } else if (dynamics.isEmpty()) {
        item(key = "dynamic-empty") { SpaceEmpty("暂无动态") }
    } else {
        items(dynamics, key = { "dynamic-${it.id}" }) { item ->
            FeedCard(
                item = item,
                actions = com.aritxonly.bdfun.ui.FeedCardActions(
                    onOpen = { navigateToFeed(context, item) },
                    onOpenVideo = { navigateToVideo(context, it) },
                    onLike = {},
                    onComment = { navigateToFeed(context, item) },
                    onRepost = {}
                ),
                presentation = com.aritxonly.bdfun.ui.FeedCardPresentation(liked = false),
                modifier = Modifier
                    .then(tabEnterModifier)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        if (dynamicLoading) {
            item(key = "dynamic-loading-more") { SpaceLoadingRow() }
        }
    }
}
