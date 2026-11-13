package com.aritxonly.bdfun.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.aritxonly.teak.common.navigation.TeakMorphMenuHost
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

@Composable
internal fun LiveRoomScreen(
    content: LiveRoomContent,
    actions: LiveRoomActions,
    player: com.aritxonly.bdfun.feature.live.presentation.LiveVideoOutput?,
    modifier: Modifier = Modifier,
) {
    val room = content.room
    val context = LocalContext.current
    val view = LocalView.current
    val landscape = LocalConfiguration.current.screenWidthDp >
        LocalConfiguration.current.screenHeightDp
    val menuState = remember { TeakMorphMenuState() }
    val follow = remember(room.roomId) { LiveFollowStateHolder(room.following) }
    val ui = remember { LiveRoomUiStateHolder() }
    val render = LiveRoomRender(content, actions, player, menuState, ui, follow)

    // 全屏横屏时和视频播放器一样隐藏状态栏/导航栏；回竖屏恢复
    LaunchedEffect(landscape) {
        val window = context.findLiveActivity()?.window ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(window, view)
        if (landscape) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        if (landscape) render.LandscapeLiveLayout() else render.PortraitLiveLayout()
        render.LiveRoomPanels(landscape)
        render.LiveRoomMenu()
    }
}

@Composable
internal fun LiveRoomRender.LiveRoomPanels(landscape: Boolean) {
        if (ui.settingsOpen) {
            LiveSettingsOverlay(
                landscape = landscape,
                qualities = qualities,
                currentQuality = currentQuality,
                onSelectQuality = { qn ->
                    ui.settingsOpen = false
                    onSelectQuality(qn)
                },
                onDismiss = { ui.settingsOpen = false },
            )
        }

        if (ui.danmakuSettingsOpen) {
            LiveDanmakuSettingsOverlay(
                landscape = landscape,
                onDismiss = { ui.danmakuSettingsOpen = false },
            )
        }

}

@Composable
internal fun LiveRoomRender.LiveRoomMenu() {
    val context = LocalContext.current
    TeakMorphMenuHost(menuState) { dismiss ->
        LiveMenuRow("复制直播间链接") {
            val url = "https://live.bilibili.com/${room.roomId}"
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("直播间链接", url))
            dismiss { context.showToast("已复制直播间链接") }
        }
        LiveMenuRow("在浏览器中打开") {
            dismiss {
                runCatching {
                    context.startActivity(
                        android.content.Intent(
                            android.content.Intent.ACTION_VIEW,
                            android.net.Uri.parse("https://live.bilibili.com/${room.roomId}"),
                        ),
                    )
                }.onFailure { context.showToast("无法打开链接") }
            }
        }
        LiveMenuRow("举报直播间") {
            dismiss { context.showToast("举报功能稍后接入") }
        }
    }
}
