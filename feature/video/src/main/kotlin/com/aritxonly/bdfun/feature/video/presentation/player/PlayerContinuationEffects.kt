package com.aritxonly.bdfun.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.core.domain.PlaybackContinuation
import com.aritxonly.bdfun.core.domain.playbackContinuation
import kotlinx.coroutines.delay

@Composable
fun BindPlayerContinuation(
    playback: PlaybackController,
    fullscreenInfo: FullscreenPlayerInfo,
    onPageSelected: (com.aritxonly.bdfun.core.model.video.VideoPage) -> Unit,
    ui: VideoPlayerUiStateHolder, page: PlayerPageHost, hasNext: () -> Boolean, toast: (String) -> Unit,
) {
    val state = playback.state.collectAsState()
    val ended by remember(playback) { derivedStateOf { state.value.ended } }
    val playerViewRef by ui.playerViewRef
    val fsResizeMode by ui.fsResizeMode
    val fsLoopMode by ui.fsLoopMode
    val fsAutoPlayNext by ui.fsAutoPlayNext
    val fsHasNext = run {
        val pages = fullscreenInfo.pages
        val index = pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        pages.size > 1 && index >= 0 && index + 1 < pages.size
    }
    // 分 P 属于同一个稿件，下一项仍是分 P 时才算“同稿件的下一集”；
    // 越过 partCount 之后就是合集里的别的稿件，不能当分 P 处理。
    val fsHasNextPart = run {
        val index = fullscreenInfo.pages.indexOfFirst { it.cid == fullscreenInfo.currentCid }
        fsHasNext && index >= 0 && index + 1 < fullscreenInfo.partCount
    }
    // 有合集才谈得上“合集放完回到合集开头”，单集视频仍然走来源列表。
    val fsHasCollection = fullscreenInfo.collectionStartIndex >= 0 && fullscreenInfo.pages.size > 1
    val fsCollectionStart = fullscreenInfo.collectionStartIndex.coerceIn(0, fullscreenInfo.pages.lastIndex.coerceAtLeast(0))
    LaunchedEffect(fsResizeMode) {
        playerViewRef?.resizeMode = fsResizeMode
    }

    // 播完往哪走由「循环播放」决定，策略在 core:domain 里单独测过。
    LaunchedEffect(ended, fsLoopMode, fsAutoPlayNext, fsHasNext, fsHasNextPart, fsHasCollection) {
        if (!ended) return@LaunchedEffect
        val queued = hasNext()
        when (
            playbackContinuation(
                loopMode = fsLoopMode,
                autoPlayNext = fsAutoPlayNext,
                hasNextPage = fsHasNext,
                hasNextPart = fsHasNextPart,
                hasNextInQueue = queued,
                hasCollection = fsHasCollection,
            )
        ) {
            PlaybackContinuation.RepeatSingle -> {
                playback.seekTo(0L)
                playback.play()
                playback.acknowledgeEnd()
            }
            PlaybackContinuation.NextPage -> onPageSelected(
                fullscreenInfo.pages[
                    (fullscreenInfo.pages.indexOfFirst { it.cid == fullscreenInfo.currentCid } + 1)
                        .coerceAtMost(fullscreenInfo.pages.lastIndex)
                ],
            )
            PlaybackContinuation.RestartCollection -> onPageSelected(fullscreenInfo.pages[fsCollectionStart])
            PlaybackContinuation.NextInQueue -> {
                playback.acknowledgeEnd()
                page.requestAutoNext()
            }
            PlaybackContinuation.Stop -> Unit
        }
    }

    BindPlayerSleepTimer(playback, ui, toast)
}

@Composable
private fun BindPlayerSleepTimer(playback: PlaybackController, ui: VideoPlayerUiStateHolder, toast: (String) -> Unit) {
    var fsSleepAt by ui.fsSleepAt
    // 定时关闭
    LaunchedEffect(fsSleepAt) {
        if (fsSleepAt <= 0L) return@LaunchedEffect
        while (true) {
            val left = fsSleepAt - System.currentTimeMillis()
            if (left <= 0L) {
                playback.pause()
                fsSleepAt = 0L
                toast("定时关闭已生效")
                break
            }
            delay(1000L)
        }
    }

}
