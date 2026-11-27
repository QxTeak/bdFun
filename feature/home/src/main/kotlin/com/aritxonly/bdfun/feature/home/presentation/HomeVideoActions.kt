package com.aritxonly.bdfun.feature.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.common.messageOr
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Text

/**
 * 省略号 / 长按共用的形变菜单宿主。挂在主壳玻璃采样树之外（MainHomeChrome），
 * 否则玻璃会采样到包含自己的那一帧，RenderThread 递归直接闪退。
 */
@Composable
fun HomeVideoMorphHost(state: HomeFeedState, onMessage: (String) -> Unit) {
    var activeState by remember { mutableStateOf<TeakMorphMenuState?>(null) }
    var lastVideo by remember { mutableStateOf<VideoItem?>(null) }
    LaunchedEffect(state.menuState, state.menuVideo) {
        if (state.menuState != null) activeState = state.menuState
        if (state.menuVideo != null) lastVideo = state.menuVideo
    }
    val menuState = activeState ?: return
    val video = state.menuVideo ?: lastVideo
    val scope = rememberCoroutineScope()
    val actions = state.actions
    val observed by actions.state.collectAsState()
    val uiState = HomeActionSheetUiState(
        title = video?.title.orEmpty(), cover = video?.cover.orEmpty(),
        creator = video?.ownerName.orEmpty(), category = observed.category,
        hasCreator = (video?.ownerMid ?: 0L) > 0,
        categoryLoading = observed.categoryLoading,
        localCategoryOnly = !observed.categoryLoading && observed.category.isNotBlank() && observed.categoryReason == null,
        busy = observed.busy,
    )
    val context = LocalContext.current
    val busy = observed.busy
    // 菜单打开时把视频交给业务控制器；否则 perform() 拿不到 target，所有项都会静默无响应。
    LaunchedEffect(video?.bvid) {
        if (video != null) actions.select(video)
    }
    val clearMenu: () -> Unit = {
        state.menuVideo = null
        state.menuState = null
    }
    // 点任意一项都先把面板关掉，再在后台执行动作；动作结束后彻底卸载宿主。
    val closeThen: (suspend () -> Unit) -> Unit = { after ->
        menuState.close()
        scope.launch {
            kotlinx.coroutines.delay(240)
            try { after() } finally { clearMenu() }
        }
    }
    val perform: (HomeVideoAction) -> Unit = { action ->
        closeThen {
            when (val result = actions.perform(action)) {
                is Outcome.Success -> {
                    // “不感兴趣”不再弹 toast：提示直接显示在模糊封面上，和撤销入口在同一张卡里。
                    if (action == HomeVideoAction.HideVideo) {
                        video?.let { state.markUndo(it, result.value) }
                    } else {
                        onMessage(result.value)
                    }
                }
                is Outcome.Failure -> onMessage(result.error.messageOr("操作失败，请重试"))
                null -> Unit
            }
        }
    }
    TeakMorphMenuHost(menuState, onDismiss = clearMenu, minWidth = 220.dp, maxWidth = 300.dp) { _ ->
        HomeVideoMenuContent(video, busy, context, perform, closeThen, onMessage)
    }
}

@Composable
private fun HomeVideoMenuContent(
    video: VideoItem?,
    busy: Boolean,
    context: android.content.Context,
    perform: (HomeVideoAction) -> Unit,
    closeThen: (suspend () -> Unit) -> Unit,
    onMessage: (String) -> Unit,
) {
    // 首行 BV 号：点一下复制；第二行是发布日期，只读。
    GlassPopupItem(
        text = video?.bvid.orEmpty(),
        enabled = video != null,
        onClick = {
            val bvid = video?.bvid.orEmpty()
            if (bvid.isNotBlank()) {
                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("BV 号", bvid))
                onMessage("已复制 BV 号")
            }
            closeThen { }
        },
    )
    // 作为正常菜单项出现，不是夹在中间的小字。
    GlassPopupItem(
        text = "发布于 ${homePublishDate(video?.pubdate ?: 0L)}",
        enabled = true,
        onClick = { closeThen { } },
    )
    GlassPopupItem(
        text = "添加至稍后再看",
        enabled = !busy,
        onClick = { perform(HomeVideoAction.WatchLater) },
    )
    GlassPopupItem(
        text = "不感兴趣",
        enabled = !busy,
        onClick = { perform(HomeVideoAction.HideVideo) },
    )
    GlassPopupItem(
        text = "拉黑 ${video?.ownerName.orEmpty()}",
        enabled = !busy && (video?.ownerMid ?: 0L) > 0,
        contentColor = Color(0xFFE5484D),
        onClick = { perform(HomeVideoAction.BlockCreator) },
    )
}

/** 动态/首页的发布日期只展示到天，避免把时间戳原样丢给用户。 */
private fun homePublishDate(seconds: Long): String {
    if (seconds <= 0L) return "未知"
    return runCatching {
        java.time.Instant.ofEpochSecond(seconds)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
    }.getOrDefault("未知")
}

