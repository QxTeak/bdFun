package com.aritxonly.bdfun.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aritxonly.bdfun.core.model.live.LiveChatMessage
import com.aritxonly.bdfun.core.model.live.LiveRoomInfo
import com.aritxonly.teak.common.navigation.TeakMorphMenuState

internal data class LiveRoomContent(
    val room: LiveRoomInfo,
    val messages: List<LiveChatMessage>,
    val statusText: String?,
    val qualities: List<Pair<Int, String>>,
    val currentQuality: Int,
    val roomEmotes: Map<String, String>,
    val isPlaying: Boolean,
    val danmakuEnabled: Boolean,
)

internal data class LiveRoomActions(
    val onSend: (String) -> Unit,
    val onBack: () -> Unit,
    val onSelectQuality: (Int) -> Unit,
    val onTogglePlay: () -> Unit,
    val onToggleDanmaku: () -> Unit,
    val onToggleFullscreen: () -> Unit,
    val onReload: () -> Unit,
)

internal class LiveRoomUiStateHolder {
    var settingsOpen by mutableStateOf(false)
    var danmakuSettingsOpen by mutableStateOf(false)
    /** 宽屏分屏里点全屏：不转屏，只把右侧栏收起。 */
    var wideFullscreen by mutableStateOf(false)
}
internal class LiveFollowStateHolder(initial: Boolean) {
    var following by mutableStateOf(initial)
}
internal class LiveInputStateHolder {
    var text by mutableStateOf("")
    var inputFocused by mutableStateOf(false)
}
internal class LiveRoomRender(
    val content: LiveRoomContent,
    val actions: LiveRoomActions,
    val player: com.aritxonly.bdfun.feature.live.presentation.LiveVideoOutput?,
    val menuState: TeakMorphMenuState,
    val ui: LiveRoomUiStateHolder,
    val follow: LiveFollowStateHolder,
) {
    val room get() = content.room
    val messages get() = content.messages
    val statusText get() = content.statusText
    val qualities get() = content.qualities
    val currentQuality get() = content.currentQuality
    val roomEmotes get() = content.roomEmotes
    val isPlaying get() = content.isPlaying
    val danmakuEnabled get() = content.danmakuEnabled
    val onSend get() = actions.onSend
    val onBack get() = actions.onBack
    val onSelectQuality get() = actions.onSelectQuality
    val onTogglePlay get() = actions.onTogglePlay
    val onToggleDanmaku get() = actions.onToggleDanmaku
    val onToggleFullscreen get() = actions.onToggleFullscreen
    val onReload get() = actions.onReload
    var following: Boolean
        get() = follow.following
        set(value) { follow.following = value }
    val settingsOpen get() = ui.settingsOpen || ui.danmakuSettingsOpen
    val onFollow: () -> Unit = { following = !following }
    val onOpenSettings: () -> Unit = { ui.settingsOpen = true }
    val onOpenDanmakuSettings: () -> Unit = { ui.danmakuSettingsOpen = true }
}
