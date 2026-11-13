package com.aritxonly.bdfun

import com.aritxonly.bdfun.ui.ActivityDiscOptions
import com.aritxonly.bdfun.ui.ActivityPageOptions
import com.aritxonly.bdfun.ui.ActivityPageSlots

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.feed.FeedItem
import com.aritxonly.bdfun.ui.CommentReplyBar
import com.aritxonly.bdfun.ui.LocalVideoPageState
import com.aritxonly.bdfun.ui.TeakActivityScaffold

@Composable
internal fun FeedDetailContent(dynamic: FeedItem, onBack: () -> Unit) {
    val pageScope = rememberCoroutineScope()
    val pageState = remember { AppGraph.videoPageState(pageScope) }
    CompositionLocalProvider(LocalVideoPageState provides pageState) {
        TeakActivityScaffold(
            settings = AppGraph.settings,
            onBack = onBack,
            options = ActivityPageOptions(
                title = "动态详情",
            ),
            disc = ActivityDiscOptions(
                discLift = 74.dp,
                discEndPadding = 0.dp,
            ),
            slots = ActivityPageSlots(
                overlay = { CommentReplyBar(modifier = Modifier.align(Alignment.BottomCenter)) },
            ),
        ) {
            FeedDetailScreen(dynamic = dynamic, onBack = onBack)
        }
    }
}
