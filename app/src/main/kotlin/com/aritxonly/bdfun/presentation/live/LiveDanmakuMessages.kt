package com.aritxonly.bdfun.ui

import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.android.purebilibili.danmaku.engine.DANMAKU_LAYER_SCROLL
import com.android.purebilibili.danmaku.engine.DanmakuItem
import com.aritxonly.bdfun.core.model.live.LiveChatKind
import com.aritxonly.bdfun.core.model.live.LiveChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

private const val LIVE_DM_POLL_MS = 100L
@Composable
internal fun LiveDanmakuMessages(engine: com.android.purebilibili.danmaku.engine.DanmakuEngine, messages: List<LiveChatMessage>, playing: Boolean,
    textSizePx: Float, roomEmotes: Map<String, String>, ui: LiveDanmakuUiStateHolder) {
    val context = LocalContext.current
    val settings = LocalSettingsBindings.current
    var lastSeenId by ui::lastSeenId
    val latestMessages by rememberUpdatedState(messages)
    val latestPlaying by rememberUpdatedState(playing)
    val latestTextSizePx by rememberUpdatedState(textSizePx)
    val latestRoomEmotes by rememberUpdatedState(roomEmotes)
    val scope = rememberCoroutineScope()
    val emoteLoadBudget = remember { Semaphore(4) }

    LaunchedEffect(engine) {
        while (isActive) {
            val pending = latestMessages.asSequence()
                .filter { it.id > lastSeenId && it.kind == LiveChatKind.DANMAKU }
                .filter { item ->
                    if (item.text.isBlank()) return@filter false
                    if (settings.danmakuBlockScroll) return@filter false
                    if (settings.danmakuKeywordFilter &&
                        settings.danmakuKeywords.any { it.isNotBlank() && item.text.contains(it) }
                    ) {
                        return@filter false
                    }
                    true
                }
                .toList()
            if (pending.isNotEmpty()) {
                lastSeenId = pending.last().id
                if (latestPlaying) {
                    // 直播间表情快照在整批内保持同一引用，正则缓存才能命中。
                    val roomEmotes = latestRoomEmotes
                    pending.forEach { message ->
                        val rgb = message.nameColor?.toInt()?.and(0xFFFFFF) ?: 0xFFFFFF
                        val textColor = 0xFF000000.toInt() or rgb
                        val extraEmote = message.emoteUrl
                            ?.takeIf { it.isNotBlank() }
                            ?.let { message.text to it }
                        if (roomEmotes.isNotEmpty() || message.emotes.isNotEmpty() || extraEmote != null) {
                            scope.launch {
                                emoteLoadBudget.withPermit {
                                    val item = withContext(Dispatchers.IO) {
                                        buildLiveEmoteDanmaku(
                                            context = context,
                                            text = message.text,
                                            textColor = textColor,
                                            textSizePx = latestTextSizePx,
                                            emotes = roomEmotes,
                                            emoteHeights = message.emoteHeights,
                                            extraEmotes = message.emotes,
                                            extraEmote = extraEmote,
                                        )
                                    }
                                    if (item != null) engine.addImmediate(item)
                                }
                            }
                        } else {
                            engine.addImmediate(
                                DanmakuItem().apply {
                                    text = message.text
                                    layerType = DANMAKU_LAYER_SCROLL
                                    this.textColor = textColor
                                },
                            )
                        }
                    }
                }
            } else {
                // 列表被截断或重载时，避免旧 id 一直卡住
                val newest = latestMessages.lastOrNull()?.id ?: 0L
                if (newest > lastSeenId) lastSeenId = newest
            }
            delay(LIVE_DM_POLL_MS)
        }
    }
}
