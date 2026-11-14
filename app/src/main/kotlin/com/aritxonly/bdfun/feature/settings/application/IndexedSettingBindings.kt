package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.SettingsRepository
import com.aritxonly.bdfun.core.model.settings.SettingValue

internal data class IndexedSettingBinding(val read: () -> SettingValue, val write: (SettingValue) -> Unit)

/** 保留原控制器的约束和通知；动作与独立存储不伪装为 settings 偏好。 */
internal fun indexedSettingBindings(repository: SettingsRepository): Map<String, IndexedSettingBinding> =
    appearanceBindings(repository) + playbackBindings(repository) + otherBindings(repository)

private fun appearanceBindings(repository: SettingsRepository) = mapOf(
    "dark_mode" to IndexedSettingBinding({ SettingValue.Number(repository.appearance.darkMode) },
        { repository.appearance.darkMode = (it as SettingValue.Number).value }),
    "color_style" to IndexedSettingBinding({ SettingValue.Number(repository.appearance.colorStyle) },
        { repository.appearance.colorStyle = (it as SettingValue.Number).value }),
    "interface_style" to IndexedSettingBinding({ SettingValue.Number(repository.appearance.interfaceStyle) },
        { repository.appearance.interfaceStyle = (it as SettingValue.Number).value }),
    "blur" to IndexedSettingBinding({ SettingValue.Flag(repository.appearance.blurEnabled) },
        { repository.appearance.blurEnabled = (it as SettingValue.Flag).value }),
    "animations_enabled" to IndexedSettingBinding({ SettingValue.Flag(repository.appearance.animationsEnabled) },
        { repository.appearance.animationsEnabled = (it as SettingValue.Flag).value }),
    "bottom_bar_raise" to IndexedSettingBinding({ SettingValue.Flag(repository.appearance.bottomBarRaise) },
        { repository.appearance.bottomBarRaise = (it as SettingValue.Flag).value }),
    "scroll_top_enabled" to IndexedSettingBinding({ SettingValue.Flag(repository.appearance.scrollTopEnabled) },
        { repository.appearance.scrollTopEnabled = (it as SettingValue.Flag).value }),
    "scroll_top_side" to IndexedSettingBinding({ SettingValue.Number(repository.appearance.scrollTopSide) },
        { repository.appearance.scrollTopSide = (it as SettingValue.Number).value }),
    "font_scale" to IndexedSettingBinding({ SettingValue.Decimal(repository.appearance.fontScale) },
        { repository.appearance.fontScale = (it as SettingValue.Decimal).value }),
    "scroll_top_pages" to IndexedSettingBinding({ SettingValue.TextSet(repository.appearance.scrollTopPages) },
        { repository.appearance.scrollTopPages = (it as SettingValue.TextSet).value }),
)

private fun playbackBindings(repository: SettingsRepository) = mapOf(
    "background_play" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.backgroundPlay) },
        { repository.playback.backgroundPlay = (it as SettingValue.Flag).value }),
    "resume_playback" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.resumePlayback) },
        { repository.playback.resumePlayback = (it as SettingValue.Flag).value }),
    "gesture_brightness_volume" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.gestureBrightnessVolume) },
        { repository.playback.gestureBrightnessVolume = (it as SettingValue.Flag).value }),
    "gesture_center_fullscreen" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.gestureCenterFullscreen) },
        { repository.playback.gestureCenterFullscreen = (it as SettingValue.Flag).value }),
    "gesture_boost_swipe" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.gestureBoostSwipe) },
        { repository.playback.gestureBoostSwipe = (it as SettingValue.Flag).value }),
    "boost_default_speed" to IndexedSettingBinding({ SettingValue.Decimal(repository.playback.boostDefaultSpeed) },
        { repository.playback.boostDefaultSpeed = (it as SettingValue.Decimal).value }),
    "default_playback_speed" to IndexedSettingBinding({ SettingValue.Decimal(repository.playback.defaultPlaybackSpeed) },
        { repository.playback.defaultPlaybackSpeed = (it as SettingValue.Decimal).value }),
    "show_fullscreen_time" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.showFullscreenTime) },
        { repository.playback.showFullscreenTime = (it as SettingValue.Flag).value }),
    "show_fullscreen_actions" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.showFullscreenActions) },
        { repository.playback.showFullscreenActions = (it as SettingValue.Flag).value }),
    "progress_bar_mode" to IndexedSettingBinding({ SettingValue.Number(repository.playback.progressBarMode) },
        { repository.playback.progressBarMode = (it as SettingValue.Number).value }),
    "quality" to IndexedSettingBinding({ SettingValue.Number(repository.playback.qualityPreference) },
        { repository.playback.qualityPreference = (it as SettingValue.Number).value }),
    "decoder_mode" to IndexedSettingBinding({ SettingValue.Number(repository.playback.decoderMode) },
        { repository.playback.decoderMode = (it as SettingValue.Number).value }),
    "codec_preference" to IndexedSettingBinding({ SettingValue.Text(repository.playback.codecPreference) },
        { repository.playback.codecPreference = (it as SettingValue.Text).value }),
    "cdn_service" to IndexedSettingBinding({ SettingValue.Text(repository.playback.cdnService) },
        { repository.playback.cdnService = (it as SettingValue.Text).value }),
    "auto_rotate" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.autoRotate) },
        { repository.playback.autoRotate = (it as SettingValue.Flag).value }),
    "auto_play" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.autoPlay) },
        { repository.playback.autoPlay = (it as SettingValue.Flag).value }),
    "loudness_eq" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.loudnessEq) },
        { repository.playback.loudnessEq = (it as SettingValue.Flag).value }),
    "bt_latency_enabled" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.bluetoothLatencyEnabled) },
        { repository.playback.bluetoothLatencyEnabled = (it as SettingValue.Flag).value }),
    "bt_latency_ms" to IndexedSettingBinding({ SettingValue.Number(repository.playback.bluetoothLatencyMs) },
        { repository.playback.bluetoothLatencyMs = (it as SettingValue.Number).value }),
    "immersive_status" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.videoImmersiveStatusBar) },
        { repository.playback.videoImmersiveStatusBar = (it as SettingValue.Flag).value }),
    "sponsor_skip" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.sponsorSkipEnabled) },
        { repository.playback.sponsorSkipEnabled = (it as SettingValue.Flag).value }),
    "dynamic_auto_play" to IndexedSettingBinding({ SettingValue.Flag(repository.playback.dynamicAutoPlay) },
        { repository.playback.dynamicAutoPlay = (it as SettingValue.Flag).value }),
    "dynamic_auto_play_quality" to IndexedSettingBinding({ SettingValue.Number(repository.playback.dynamicAutoPlayQuality) },
        { repository.playback.dynamicAutoPlayQuality = (it as SettingValue.Number).value }),
)

private fun otherBindings(repository: SettingsRepository) = mapOf(
    "smart_refresh" to IndexedSettingBinding({ SettingValue.Flag(repository.general.smartRefresh) },
        { repository.general.smartRefresh = (it as SettingValue.Flag).value }),
    "show_viewer_count" to IndexedSettingBinding({ SettingValue.Flag(repository.general.showViewerCount) },
        { repository.general.showViewerCount = (it as SettingValue.Flag).value }),
    "show_ip_location" to IndexedSettingBinding({ SettingValue.Flag(repository.message.showIpLocation) },
        { repository.message.showIpLocation = (it as SettingValue.Flag).value }),
    "hot_danmaku" to IndexedSettingBinding({ SettingValue.Flag(repository.danmaku.hotDanmakuEnabled) },
        { repository.danmaku.hotDanmakuEnabled = (it as SettingValue.Flag).value }),
    "dm_low_refresh" to IndexedSettingBinding({ SettingValue.Flag(repository.danmaku.danmakuLowRefresh) },
        { repository.danmaku.danmakuLowRefresh = (it as SettingValue.Flag).value }),
    "show_hot_search" to IndexedSettingBinding({ SettingValue.Flag(repository.general.showHotSearch) },
        { repository.general.showHotSearch = (it as SettingValue.Flag).value }),
    "sent_history_entry" to IndexedSettingBinding({ SettingValue.Flag(repository.message.sentHistoryEntry) },
        { repository.message.sentHistoryEntry = (it as SettingValue.Flag).value }),
)
