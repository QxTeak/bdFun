package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp

@Composable
internal fun LiveRoomRender.PortraitLiveLayout() {
    val density = LocalDensity.current
    val imeBottom = with(density) { WindowInsets.ime.getBottom(density).toDp() }
    val navBottom = with(density) { WindowInsets.navigationBars.getBottom(density).toDp() }
    val bottomInset = if (imeBottom > navBottom) imeBottom else navBottom
    // 互动区要给悬浮输入条让出高度：44dp 输入框 + 上下 8dp 内边距 + 底部系统留白
    val inputAreaHeight = 44.dp + 16.dp + bottomInset
    val inputState = remember { LiveInputStateHolder() }
    var inputFocused by inputState::inputFocused
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(Modifier.fillMaxSize()) {
        // 背景：直播间封面放大模糊，避免播放器下方纯黑一块
        BackdropGlow(room, Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            // ---- 顶栏单独占一条（避开状态栏/前摄），播放器从它下面开始 ----
            // BiliPai 的 LiveRoomLayout 就是 header 放在 0,0，播放器 y = header 高度。
            PortraitLiveHeader()

            // ---- 播放器区（16:9）----
            PortraitLivePlayer()

            // ---- 互动区：只放弹幕，底部给悬浮输入条让位 ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(bottom = inputAreaHeight),
            ) {
                LiveChatList(
                    messages = messages,
                    enabled = danmakuEnabled,
                    modifier = Modifier.fillMaxSize(),
                )
                // 播放器下沿到互动区的过渡：参考视频页的渐变遮罩，直播只用深色
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.86f),
                                0.55f to Color.Black.copy(alpha = 0.38f),
                                1f to Color.Transparent,
                            ),
                        ),
                )
            }
        }

        // 输入框聚焦时铺一层透明点击层：点画面或空白处收起键盘
        if (inputFocused) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        focusManager.clearFocus()
                        keyboard?.hide()
                    },
            )
        }

        // 输入条：单独悬浮在页面底部，背后是页面背景，没有黑底
        LiveInputBar(
            onSend = onSend,
            onFocusChanged = { inputFocused = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
        )
    }
}

/* ------------------------------------------------------------------ */
/* 横屏：播放器铺满，弹幕浮在左下                                       */
/* ------------------------------------------------------------------ */


@Composable
internal fun LiveRoomRender.PortraitLivePlayer() {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
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
                    fullscreen = false,
                    roomEmotes = roomEmotes,
                    modifier = Modifier.fillMaxSize(),
                )
                LivePlayerChrome(
                    content = LiveChromeContent(false, isPlaying, danmakuEnabled, qualities.firstOrNull { it.first == currentQuality }?.second ?: "清晰度", room.title, settingsOpen),
                    actions = LiveChromeActions(onTogglePlay, onReload, onToggleDanmaku, onOpenSettings, onOpenDanmakuSettings, onToggleFullscreen, onBack),
                )
            }

}

@Composable
private fun LiveRoomRender.PortraitLiveHeader() {
    Box(
        // 顶栏也铺页面背景（BackdropGlow），只叠一层自上而下的深色渐变保证文字可读。
        // 原来这里是纯黑色块，和下半部分有背景的画面之间会切出一条硬接缝。
        Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                ),
            ),
    ) {
        LiveTopBar(
            room = room,
            following = following,
            menuState = menuState,
            onBack = onBack,
            onFollow = onFollow,
        )
    }

}
