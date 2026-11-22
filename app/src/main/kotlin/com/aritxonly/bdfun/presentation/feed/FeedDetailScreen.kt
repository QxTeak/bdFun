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
                // 动态作者就是评论区的 UP 主：和视频页一样按 mid 打 UP 标识。
                upMid = detail.mid,
                title = detail.name,
                bvid = "",
                commentOid = detail.commentOid.ifBlank { detail.id },
                commentType = detail.commentType,
            ),
            onBack = onBack,
            // 和视频页评论区完全同一套：平铺、无圆角卡片、同样的间距和骨架屏。
            layout = CommentScreenLayout(
                bare = true,
                topInset = 4.dp,
                bottomPadding = 96.dp,
                trackBackProgress = true,
                flatList = true,
                compactHeaderSpacing = true,
                scrollTop = true,
                // 动态详情要沉浸：列表滚到状态栏下面，不留下一条挡住内容的空白。
                immersiveStatusBar = true,
            ),
            header = { dynamicDetailHeader(snapshot, actions) },
        )
}
