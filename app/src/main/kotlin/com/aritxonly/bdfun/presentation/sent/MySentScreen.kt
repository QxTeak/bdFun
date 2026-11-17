package com.aritxonly.bdfun.ui

import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.sent.application.*
import androidx.compose.runtime.collectAsState

@Composable
internal fun MySentScreen(controller: SentHistoryStateHolder, onOpenVideo: (VideoItem) -> Unit) {
    val context = LocalContext.current
    val state by controller.state.collectAsState()
    val shown = state.shown
    val listState = rememberLazyListState()
    TrackTeakPageScroll(listState)
    val classic = com.aritxonly.teak.common.theme.TeakUiStyle.isClassic
    val cardShape = remember(classic) {
        if (classic) androidx.compose.ui.graphics.RectangleShape else ContinuousCornerShape(18.dp)
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(
            start = if (classic) 0.dp else 16.dp,
            end = if (classic) 0.dp else 16.dp,
            top = 8.dp, bottom = 40.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(if (classic) 0.dp else 8.dp),
    ) {
        item(key = "title") { SentHistoryHeader() }
        item(key = "filters") { SentHistoryFilters(state, controller::select) }
        if (shown.isEmpty()) item(key = "empty") { SentHistoryEmpty(cardShape) }
        items(shown.size, key = { shown[it].id }) { index ->
            val record = shown[index]
            SentRecordRow(record = record, shape = cardShape, onClick = {
                val video = controller.videoFor(record)
                if (video == null) context.showToast("记录缺少视频信息") else onOpenVideo(video)
            })
        }
    }
}
