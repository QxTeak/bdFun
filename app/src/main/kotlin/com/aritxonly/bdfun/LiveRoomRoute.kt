package com.aritxonly.bdfun

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.feature.settings.presentation.LocalSettingsBindings
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.LiveRoomScreen
import com.aritxonly.bdfun.ui.LiveRoomContent
import com.aritxonly.bdfun.ui.LiveRoomActions
import com.aritxonly.teak.common.theme.TeakTheme

@Composable
internal fun LiveRoomTheme(settings: SettingsBindings, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalSettingsBindings provides settings) {
        TeakTheme(dark = true, lightColors = hyperLightColors(), darkColors = hyperDarkColors(),
            materialColors = appMaterialColors(dark = true),
            blurEnabled = settings.blurEnabled, refractionEnabled = settings.refractionEnabled, fontScale = settings.fontScale,
        ) { content() }
    }
}

@Composable
internal fun LiveRoomRoute(activity: ComponentActivity, requestedRoomId: Long) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playback = remember { AppGraph.createLivePlayback(context) }
    val connection = remember { AppGraph.liveDanmakuConnection(scope) }
    val controller = remember { AppGraph.liveRoomState(requestedRoomId, scope, connection, playback) }
    val state by controller.state.collectAsState()
    DisposableEffect(controller) { onDispose { controller.close() } }
    LaunchedEffect(requestedRoomId) { controller.loadRoom() }
    LaunchedEffect(state.content.roomId) { controller.loadEmotes() }
    LaunchedEffect(state.content.roomId, state.playback.reloadTick) { controller.receiveDanmaku() }
    LaunchedEffect(state.playback.url, playback) { state.playback.url?.let(controller::startPlayback) }
    LiveRoomWindowEffects(activity, state.playback.playing)
    LiveRoomScreen(
        content = LiveRoomContent(controller.displayRoom, state.messages, state.playback.error,
            state.content.qualities, state.playback.quality, state.content.emotes,
            state.playback.playing, state.playback.danmakuEnabled),
        actions = LiveRoomActions(controller::appendLocal, { activity.finish() },
            { qn -> if (qn != controller.state.value.playback.quality) controller.reload(qn) },
            controller::togglePlaying, controller::toggleDanmaku,
            { toggleLiveFullscreen(activity) }, { controller.reload(controller.state.value.playback.quality) }),
        player = if (state.playback.url != null) playback else null,
    )
}

@Composable
private fun LiveRoomWindowEffects(activity: ComponentActivity, playing: Boolean) {
    DisposableEffect(playing) {
        val flag = android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        if (playing) activity.window.addFlags(flag) else activity.window.clearFlags(flag)
        onDispose { activity.window.clearFlags(flag) }
    }
    BackHandler {
        if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
            activity.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else activity.finish()
    }
}

private fun toggleLiveFullscreen(activity: ComponentActivity) {
    activity.requestedOrientation = if (activity.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
        android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    } else android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}
