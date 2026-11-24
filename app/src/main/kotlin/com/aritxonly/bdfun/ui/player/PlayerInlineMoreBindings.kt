package com.aritxonly.bdfun.ui

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aritxonly.bdfun.core.application.PlaybackController
import com.aritxonly.bdfun.data.Quality
import com.aritxonly.teak.common.modal.TeakActionSheet

@Composable
internal fun PlayerInlineMore(
    playback: PlaybackController, media: VideoMediaInput, presentation: VideoPlayerPresentation,
    actions: VideoPlayerActions, fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder,
) {
    val settings = LocalVideoPageState.current.settings
    val context = LocalContext.current
    val fullscreen = presentation.fullscreen
    var halfMoreOpen by ui.halfMoreOpen
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    // 弹幕设置的二级 sheet：从「更多」里打开，跟「更多」是两个独立面板。
    var danmakuSheetOpen by remember { mutableStateOf(false) }
    var cachePickerOpen by remember { mutableStateOf(false) }
    // 选集缓存：面板里先切到“全部”，再点画质就把所有分 P / 单集一起排队。
    var cacheAll by remember { mutableStateOf(false) }
    LaunchedEffect(fullscreen) { if (fullscreen) { danmakuSheetOpen = false; cachePickerOpen = false; cacheAll = false } }
    val cacheQualities = media.info?.qualities.orEmpty().let { list ->
        list.filter { quality -> quality.id >= 64 }.ifEmpty { list }
    }
    // 半屏「更多」：与全屏 MorePanel 同一套设置项，复用评论区同款底部弹簧面板。
    if (!fullscreen) {
        HalfScreenMoreSheet(
            visible = halfMoreOpen,
            accentColor = com.aritxonly.bdfun.BrandPink,
            onDismiss = { halfMoreOpen = false },
            state = HalfMoreState(
                speed = speed,
                autoPlayNext = fsAutoPlayNext,
                loopMode = fsLoopMode,
                mirror = fsMirror,
                backgroundPlay = settings.backgroundPlay,
                subtitlesOn = fsSubtitlesOn,
                sleepAtMs = fsSleepAt,
            ),
            quick = inlineMoreQuickActions(context, cacheQualities.isNotEmpty(), actions, fsActions, ui) {
                cachePickerOpen = true
            },
            settings = inlineMoreSettingsActions(playback, ui, settings) {
                danmakuSheetOpen = true
            },
        )
        // 面板用的是主题化表面，弹幕设置这套原本的白色配色要换成主题色，否则浅色下看不见。
        TeakActionSheet(
            visible = danmakuSheetOpen,
            onDismiss = { danmakuSheetOpen = false },
            title = "弹幕设置",
        ) {
            CompositionLocalProvider(LocalDanmakuPanelPalette provides themedDanmakuPanelPalette()) {
                DanmakuSettingsContent(
                    fullscreen = false,
                    onInteract = {},
                    danmakuItems = media.danmaku,
                ) { DanmakuCloudRulesEntry({}) }
            }
        }
        // 缓存画质：复用全 App 同一套底部 ActionSheet，不另起一个对话框样式。
        CacheQualitySheet(
            visible = cachePickerOpen,
            cacheQualities = cacheQualities,
            episodeCount = media.cacheEpisodeCount,
            cacheAll = cacheAll,
            onToggleAll = { cacheAll = !cacheAll },
            onDismiss = { cachePickerOpen = false; cacheAll = false },
            onPick = { item, all ->
                if (all) actions.transport.onCacheAll(item) else actions.transport.onCacheVideo(item)
                cachePickerOpen = false
                cacheAll = false
            },
        )
    }
}

/** 缓存画质 / 选集面板：与全 App 共用同一套底部 ActionSheet 外壳。 */
@Composable
private fun CacheQualitySheet(
    visible: Boolean,
    cacheQualities: List<Quality>,
    episodeCount: Int,
    cacheAll: Boolean,
    onToggleAll: () -> Unit,
    onDismiss: () -> Unit,
    onPick: (Quality, Boolean) -> Unit,
) {
    TeakActionSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = if (cacheAll) "缓存全部 $episodeCount 集" else "选择缓存画质",
    ) {
        if (episodeCount > 1) {
            ActionSheetRow(
                label = if (cacheAll) "改为只缓存当前集" else "改为缓存全部 $episodeCount 集",
            ) { onToggleAll() }
        }
        if (cacheQualities.isEmpty()) {
            androidx.compose.material3.Text(
                "暂无可用清晰度",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            )
        }
        cacheQualities.forEach { item ->
            ActionSheetRow(label = qualityMenuLabel(item.label)) { onPick(item, cacheAll) }
        }
    }
}

private fun inlineMoreQuickActions(    context: Context, hasCacheQuality: Boolean, actions: VideoPlayerActions,
    fsActions: FullscreenActions, ui: VideoPlayerUiStateHolder, onRequestCache: () -> Unit,
): HalfMoreQuickActions {
    var halfMoreOpen by ui.halfMoreOpen
    val onDislikeClick = actions.interaction.onDislikeClick
    val onAddWatchLater = actions.interaction.onAddWatchLater
    return HalfMoreQuickActions(
        onDislike = {
            onDislikeClick()
            context.showToast("已减少此类推荐")
        },
        onWatchLater = {
            onAddWatchLater()
            halfMoreOpen = false
        },
        onPictureInPicture = {
            fsActions.onPictureInPicture()
            halfMoreOpen = false
        },
        onCast = {
            fsActions.onCast()
            halfMoreOpen = false
        },
        onCache = {
            if (!hasCacheQuality) context.showToast("暂无可用清晰度") else onRequestCache()
            halfMoreOpen = false
        },
    )
}

private fun inlineMoreSettingsActions(
    playback: PlaybackController, ui: VideoPlayerUiStateHolder,
    settings: com.aritxonly.bdfun.core.domain.SettingsRepository,
    onOpenDanmakuSettings: () -> Unit,
): HalfMoreSettingsActions {
    var speed by ui.speed
    var fsAutoPlayNext by ui.fsAutoPlayNext
    var fsLoopMode by ui.fsLoopMode
    var fsMirror by ui.fsMirror
    var fsSubtitlesOn by ui.fsSubtitlesOn
    var fsSleepAt by ui.fsSleepAt
    return HalfMoreSettingsActions(
        onSpeed = {
            speed = it
            playback.setSpeed(it)
        },
        onAutoPlayNext = { fsAutoPlayNext = !fsAutoPlayNext },
        onLoopMode = { fsLoopMode = it },
        onMirror = { fsMirror = !fsMirror },
        onBackgroundPlay = {
            settings.backgroundPlay = !settings.backgroundPlay
        },
        onSubtitlesToggle = { fsSubtitlesOn = !fsSubtitlesOn },
        onDanmakuSettings = onOpenDanmakuSettings,
        onSleepAt = { target ->
            fsSleepAt = target
        },
    )
}
