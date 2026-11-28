package com.aritxonly.bdfun.ui

import android.app.Activity
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.application.PlaybackPhase
import com.aritxonly.bdfun.core.domain.playbackIntentIsPlaying
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext

@Composable
fun BindPlayerLifecycle(
    playback: PlaybackController,
    page: PlayerPageHost,
    backgroundPlay: () -> Boolean,
) {
    val activity = LocalContext.current as? Activity
    DisposableEffect(playback) {
        val pauseHook: (Boolean) -> Unit = { allowResume ->
            // 相关推荐/个人主页跳转前会同步 pauseAll；记住原本正在播，
            // 回到旧页面时再恢复，而不是停在暂停态。
            // 播完时 playWhenReady 仍是 true，但那并不算“在播”，不能登记续播，否则回来会莫名重播。
            val tick = playback.refresh()
            val wasPlaying = playbackIntentIsPlaying(tick.ended, tick.playWhenReady) &&
                tick.phase != PlaybackPhase.Idle
            page.setPendingResume(if (allowResume && wasPlaying) ({ playback.play() }) else null)
            playback.pause()
        }
        page.registerPause(activity, pauseHook)
        onDispose {
            page.unregisterPause(pauseHook)
            page.setPendingResume(null)
        }
    }
    // 关闭「后台播放」后，App 退到后台立即暂停；回到前台只恢复自动暂停的那次。
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> {
                    // 新视频页启动/锁屏时，音频焦点可能在本页 onStop 之前就被抢走；
                    // 在 ON_PAUSE 先登记“原本在播”，回来才不会被误判成用户暂停。
                    val tick = playback.refresh()
                    if (playbackIntentIsPlaying(tick.ended, tick.playWhenReady)) {
                        page.setPendingResume { playback.play() }
                    }
                }
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> {
                    // 即使开着后台播放，系统/新媒体页也可能因音频焦点把原播放器暂停；
                    // 先登记续播动作，回到本页再按实际 playWhenReady 决定是否 play。
                    // 播完不算在播，否则切后台再回来会自动从头重播。
                    val tick = playback.refresh()
                    if (playbackIntentIsPlaying(tick.ended, tick.playWhenReady)) {
                        page.setPendingResume { playback.play() }
                    }
                    if (!backgroundPlay()) playback.pause()
                }
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> page.resumeIfPending()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
