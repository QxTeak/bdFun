package com.aritxonly.bdfun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.aritxonly.bdfun.data.LiveChatKind
import com.aritxonly.bdfun.data.LiveChatMessage
import com.aritxonly.bdfun.data.LiveMock
import com.aritxonly.bdfun.data.LiveRoomInfo
import com.aritxonly.bdfun.ui.LiveRoomScreen
import com.aritxonly.teak.common.theme.TeakTheme
import kotlinx.coroutines.delay

/**
 * 直播详情页。
 *
 * 数据分两步：
 * 1. 房间信息 + 播放地址走真实接口（[com.aritxonly.bdfun.data.BiliApi.liveRoomInfo] /
 *    [com.aritxonly.bdfun.data.BiliApi.livePlayUrl]），失败时退回 mock，不会白屏。
 * 2. 弹幕暂时还是本地假数据，等原生弹幕长连接做好再替换。
 */
class LiveRoomActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppGraph.ensure(applicationContext)
        val settings = AppGraph.settings
        val api = AppGraph.api
        val requestedRoomId = intent.getLongExtra(EXTRA_ROOM_ID, 0L)

        setContent {
            // 直播间始终使用深色主题：播放器区域的文字和控件都要有深色底。
            TeakTheme(
                dark = true,
                lightColors = hyperLightColors(),
                darkColors = hyperDarkColors(),
                materialColors = darkColorScheme(
                    primary = BrandPink,
                    background = Color(0xFF0D0E11),
                    surface = Color(0xFF121317),
                    surfaceVariant = Color(0xFF1A1C21),
                    onSurface = Color(0xFFF2F3F5),
                    outline = Color(0xFF2B2E35),
                ),
                blurEnabled = settings.blurEnabled,
                refractionEnabled = settings.refractionEnabled,
                fontScale = settings.fontScale,
            ) {
                val context = LocalContext.current
                var room by remember { mutableStateOf<LiveRoomInfo?>(null) }
                var playUrl by remember { mutableStateOf<String?>(null) }
                var error by remember { mutableStateOf<String?>(null) }
                var messages by remember { mutableStateOf(LiveMock.initialMessages) }

                // 播放器：进入就建好，离开页面释放
                val player = remember {
                    ExoPlayer.Builder(context.applicationContext).build().apply {
                        playWhenReady = true
                    }
                }
                DisposableEffect(Unit) {
                    onDispose { player.release() }
                }

                // 拿房间信息 + 播放地址
                LaunchedEffect(requestedRoomId) {
                    val roomId = if (requestedRoomId > 0L) {
                        requestedRoomId
                    } else {
                        runCatching { api.liveHotRoomId() }.getOrNull() ?: LiveMock.room.roomId
                    }
                    val info = runCatching { api.liveRoomInfo(roomId) }.getOrNull()
                    room = info ?: LiveMock.room.copy(roomId = roomId)
                    val url = runCatching { api.livePlayUrl(roomId) }.getOrNull()
                    playUrl = url
                    if (url == null) error = "直播流拿不到，可能未开播或接口变动"
                }

                LaunchedEffect(playUrl, player) {
                    val url = playUrl ?: return@LaunchedEffect
                    runCatching {
                        player.setMediaItem(MediaItem.fromUri(url))
                        player.prepare()
                        player.play()
                    }.onFailure { error = it.message ?: "播放失败" }
                }

                // 假弹幕：真实弹幕长连接还没接，先让界面有内容
                LaunchedEffect(Unit) {
                    while (true) {
                        delay(2200)
                        messages = (messages + LiveMock.nextMessage()).takeLast(MAX_MESSAGES)
                    }
                }

                BackHandler { finish() }

                LiveRoomScreen(
                    room = room ?: LiveMock.room.copy(roomId = requestedRoomId),
                    messages = messages,
                    onSend = { text ->
                        messages = (
                            messages + LiveChatMessage(
                                id = System.currentTimeMillis(),
                                kind = LiveChatKind.DANMAKU,
                                uname = "我",
                                text = text,
                                nameColor = 0xFF6699L,
                            )
                            ).takeLast(MAX_MESSAGES)
                    },
                    onBack = { finish() },
                    player = if (playUrl != null) player else null,
                    statusText = error,
                )
            }
        }
    }

    companion object {
        const val EXTRA_ROOM_ID = "room_id"
        private const val MAX_MESSAGES = 300
    }
}
