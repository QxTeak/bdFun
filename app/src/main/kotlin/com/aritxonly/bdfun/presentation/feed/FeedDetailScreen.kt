package com.aritxonly.bdfun

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.CommentScreenRequest
import com.aritxonly.bdfun.ui.CommentScreenLayout

@Composable
internal fun FeedDetailScreen(dynamic: FeedItem, onBack: () -> Unit) {
    val context = LocalContext.current
    val runtime = rememberFeedDetailRuntime(dynamic)
    val controller = runtime.controller
    val snapshot by androidx.compose.runtime.key(controller) { controller.state.collectAsState() }
    val detail = snapshot.detail
    LaunchedEffect(controller, dynamic.id) { controller.load() }
    LaunchedEffect(controller, detail.mid, detail.ipLocation) { controller.loadIpLocation() }
    BackHandler(onBack = onBack)
    val actions = FeedDetailActions(controller, runtime.scope, context)
    CommentScreen(
            request = CommentScreenRequest(
                aid = detail.id.toLongOrNull() ?: 0L,
                upMid = 0L,
                title = detail.name,
                bvid = "",
                commentOid = detail.commentOid.ifBlank { detail.id },
                commentType = detail.commentType,
            ),
            onBack = onBack,
            layout = CommentScreenLayout(
                bare = true,
                topInset = 4.dp,
                bottomPadding = 132.dp,
                trackBackProgress = true,
                showLoading = false,
            ),
            header = { dynamicDetailHeader(snapshot, actions) },
        )
}
