package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 平板/宽屏直播：照视频详情页的左右分屏。
 *
 * 左边是保持 16:9 画面的播放器和它自己的一套手势/控件，右边是常驻的
 * 主播信息、弹幕列表和输入条。竖屏与手机横屏仍走原来的布局，这里只新增宽屏分支。
 */
@Composable
internal fun LiveRoomRender.WideLiveLayout(
    onToggleFullscreen: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black)) {
        val detailsWidth = 360.dp.coerceAtLeast(maxWidth * 0.3f)
        Row(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                    .background(Color.Black),
            ) {
                if (player != null) {
                    LivePlayerSurface(player, Modifier.fillMaxSize())
                } else {
                    LiveVideoPlaceholder(room, statusText, Modifier.fillMaxSize())
                }
                LiveDanmakuOverlay(
                    messages = messages,
                    playing = isPlaying,
                    enabled = danmakuEnabled,
                    fullscreen = true,
                    roomEmotes = roomEmotes,
                    modifier = Modifier.fillMaxSize(),
                )
                LivePlayerChrome(
                    content = LiveChromeContent(
                        true, isPlaying, danmakuEnabled,
                        qualities.firstOrNull { it.first == currentQuality }?.second ?: "清晰度",
                        room.title, settingsOpen,
                    ),
                    actions = LiveChromeActions(
                        onTogglePlay, onReload, onToggleDanmaku, onOpenSettings,
                        onOpenDanmakuSettings, onToggleFullscreen, onBack,
                    ),
                )
            }
            Column(
                modifier = Modifier
                    .width(detailsWidth)
                    .fillMaxHeight()
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .background(MiuixTheme.colorScheme.background),
            ) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.fillMaxWidth().background(MiuixTheme.colorScheme.surfaceContainer),
                ) {
                    LiveTopBar(
                        room = room,
                        following = following,
                        menuState = menuState,
                        onBack = onBack,
                        onFollow = onFollow,
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                ) {
                    LiveChatList(
                        messages = messages,
                        enabled = danmakuEnabled,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                LiveInputBar(
                    onSend = onSend,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
