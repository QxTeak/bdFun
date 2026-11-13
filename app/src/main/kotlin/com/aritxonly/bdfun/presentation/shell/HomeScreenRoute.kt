package com.aritxonly.bdfun

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.home.presentation.HomeFeedState
import com.aritxonly.bdfun.feature.home.presentation.HomePresentationHost
import com.aritxonly.bdfun.feature.home.presentation.HomeScreen
import com.aritxonly.bdfun.ui.*

/** 只接线共享UI、平台提示与诊断；首页业务状态/页面由feature模块持有。 */
@Composable
internal fun HomeScreenRoute(state: HomeFeedState, onOpenVideo: (VideoItem) -> Unit, cardStyle: Int = 0, interactive: Boolean = true) {
    val context = LocalContext.current
    val host = remember(context) {
        HomePresentationHost(
            topBarHeight = HomeTopBarHeight,
            gridCells = { min, max -> BoundedVideoGridCells(min, max) },
            card = { video, large, onOpen, onLongClick -> HomeFeedCard(video, large, onOpen, onLongClick) },
            loading = { color, size -> TeakLoading(color = color, size = size) },
            onMessage = { context.showToast(it) },
            onEnter = { if (BuildConfig.DEBUG) com.aritxonly.bdfun.data.LiveCounters.homeInstances++ },
            onExit = { if (BuildConfig.DEBUG) com.aritxonly.bdfun.data.LiveCounters.homeInstances-- },
        )
    }
    HomeScreen(state, onOpenVideo, cardStyle, host, interactive)
}
