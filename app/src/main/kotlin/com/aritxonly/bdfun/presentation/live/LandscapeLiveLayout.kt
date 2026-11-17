package com.aritxonly.bdfun.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal fun LiveRoomRender.LandscapeLiveLayout(
    /** 宽屏分屏进入的全屏要退回分屏，而不是像手机横屏那样转回竖屏。 */
    onToggleFullscreen: () -> Unit = this.onToggleFullscreen,
) {
    Box(
        Modifier
            .fillMaxSize()
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
                onTogglePlay, onReload, onToggleDanmaku, onOpenSettings, onOpenDanmakuSettings,
                onToggleFullscreen, onToggleFullscreen,
            ),
        )
    }
}

/* ------------------------------------------------------------------ */
/* 顶栏：返回键 + 主播信息 + 省略号                                     */
/* ------------------------------------------------------------------ */
