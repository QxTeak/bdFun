package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.AppGraph
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.teak.common.navigation.LocalTeakMorphMenu
import com.aritxonly.teak.common.navigation.TeakMorphMenuItem
import com.aritxonly.teak.common.navigation.TeakMorphMenuState
import kotlinx.coroutines.launch

/**
 * 视频卡片右下角省略号 / 卡面长按的统一入口。
 *
 * 和首页同一套：按钮图标参与展开，长按从手指位置长出；真正的面板由 Activity 根部的
 * [com.aritxonly.teak.common.navigation.TeakMorphMenuProvider] 渲染，页面只提供菜单项。
 */
internal fun interface VideoCardMorphMenuHandler {
    fun show(video: VideoItem, state: TeakMorphMenuState, point: Offset?)
}

internal val LocalVideoCardMorphMenu = staticCompositionLocalOf<VideoCardMorphMenuHandler?> { null }

/** 卡片省略号的形变锚点与触发回调；打包成一个参数，避免长参数列表。 */
internal class VideoCardMorph(
    val state: TeakMorphMenuState? = null,
    val onTrigger: (() -> Unit)? = null,
)

/** 页面在列表外提供菜单内容；列表里的卡片只负责锚点。 */
@Composable
internal fun rememberVideoCardMorphMenuHandler(
    items: (VideoItem) -> List<TeakMorphMenuItem>,
): VideoCardMorphMenuHandler? {
    val menu = LocalTeakMorphMenu.current ?: return null
    val latestItems by rememberUpdatedState(items)
    return remember(menu) {
        VideoCardMorphMenuHandler { video, state, point ->
            menu.show(
                state = state,
                items = latestItems(video),
                onDismiss = {},
                showAnchorCopy = point == null,
                point = point,
            )
        }
    }
}

/** 热门 / 搜索 / 空间等页面通用的两项：稍后再看 + 分享。 */
@Composable
internal fun rememberVideoShareMorphItems(): (VideoItem) -> List<TeakMorphMenuItem> {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        { video ->
            listOf(
                TeakMorphMenuItem("添加至稍后再看") {
                    scope.launch {
                        val result = AppGraph.videoActions.watchLater(video.aid)
                        context.showToast(
                            if (result is Outcome.Failure) "添加失败，请稍后重试"
                            else "已添加至稍后再看",
                        )
                    }
                },
                TeakMorphMenuItem("分享") {
                    runCatching {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
                            .apply {
                                type = "text/plain"
                                putExtra(
                                    android.content.Intent.EXTRA_TEXT,
                                    "${video.title}\nhttps://www.bilibili.com/video/${video.bvid}",
                                )
                            }
                        context.startActivity(
                            android.content.Intent.createChooser(intent, "分享视频"),
                        )
                    }
                },
            )
        }
    }
}
