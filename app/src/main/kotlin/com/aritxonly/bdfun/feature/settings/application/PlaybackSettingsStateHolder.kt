package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.model.settings.BASE_CDN_ID
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.settings.*

internal class PlaybackSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), PlaybackSettings {
    private val adaptiveBufferingState = stateCell("adaptiveBufferingState",
        prefs.getBoolean("adaptive_buffering", com.aritxonly.bdfun.core.domain.buffering.DEFAULT_ADAPTIVE_BUFFERING))
    override var adaptiveBuffering: Boolean
        get() = adaptiveBufferingState.value
        set(value) {
            adaptiveBufferingState.value = value
            prefs.edit().putBoolean("adaptive_buffering", value).apply()
        }

    private val qualityState = stateCell("qualityState", prefs.getInt("quality", 0))

    private val lowPowerState = stateCell("lowPowerState", prefs.getBoolean("low_power", false))

    private val autoRotateState = stateCell("autoRotateState", prefs.getBoolean("auto_rotate", true))

    private val immersiveStatusState = stateCell("immersiveStatusState", prefs.getBoolean("immersive_status", false))

    private val decoderState = stateCell("decoderState", prefs.getInt("decoder_mode", 0))

    private val codecPreferenceState = stateCell("codecPreferenceState", prefs.getString("codec_preference", "auto") ?: "auto")

    // 默认用「原地址」：自动测速要下载多个节点的 1MB 样本，不能作为默认行为。
    private val cdnServiceState = stateCell("cdnServiceState",
        prefs.getString("cdn_service", BASE_CDN_ID) ?: BASE_CDN_ID,
    )

    /** 蓝牙耳机延迟补偿：检测到蓝牙输出时，让弹幕按设定延迟对齐。 */
    /** 是否允许后台播放（默认开）。关闭后离开播放器会暂停。 */
    // 弹幕显示设置：全部持久化，重启/换视频都保留

    private val stopOnVideoPageExitState = stateCell("stopOnVideoPageExitState", prefs.getBoolean("stop_on_video_page_exit", false))

    private val backgroundPlayState = stateCell("backgroundPlayState", prefs.getBoolean("background_play", true))

    private val showDiscAfterVideoState =
        stateCell("showDiscAfterVideoState", prefs.getBoolean("show_disc_after_video", true))
    /** 退出视频后控件的形态：0 = 画中画，1 = 唱片。 */

    private val exitVideoControlState =
        stateCell("exitVideoControlState", prefs.getInt("exit_video_control", 1))

    private val discBreathEnabledState =
        stateCell("discBreathEnabledState", prefs.getBoolean("disc_breath_enabled", false))

    private val discBreathCoverColorState =
        stateCell("discBreathCoverColorState", prefs.getBoolean("disc_breath_cover_color", false))

    /** 进入视频是否自动播放（默认开）。 */

    private val autoPlayState = stateCell("autoPlayState", prefs.getBoolean("auto_play", true))

    /** 播放结束后是否自动连播（默认开，需记住用户关闭状态）。 */

    private val autoPlayNextState = stateCell("autoPlayNextState", prefs.getBoolean("auto_play_next", true))

    /** 全屏循环播放：0 不循环 / 1 单集循环 / 2 列表循环。 */
    private val loopModeState = stateCell("loopModeState", prefs.getInt("loop_mode", 0))

    /** 动态页视频滚到屏幕中心附近时是否自动静音预览播放。 */

    private val dynamicAutoPlayState = stateCell("dynamicAutoPlayState", prefs.getBoolean("dynamic_auto_play", true))

    /** 动态视频自动播放时是否静音（默认静音）。 */

    private val dynamicAutoPlayMutedState =
        stateCell("dynamicAutoPlayMutedState", prefs.getBoolean("dynamic_auto_play_muted", true))

    /** 动态视频自动播放清晰度，默认 720P（64）。 */

    private val dynamicAutoPlayQualityState =
        stateCell("dynamicAutoPlayQualityState", prefs.getInt("dynamic_auto_play_quality", 64))

    private val btLatencyEnabledState = stateCell("btLatencyEnabledState", prefs.getBoolean("bt_latency_enabled", false))

    private val loudnessEqState = stateCell("loudnessEqState", prefs.getBoolean("loudness_eq", false))

    private val resumePlaybackState = stateCell("resumePlaybackState", prefs.getBoolean("resume_playback", true))

    private val gestureBrightnessVolumeState =
        stateCell("gestureBrightnessVolumeState", prefs.getBoolean("gesture_brightness_volume", true))

    private val gestureCenterFullscreenState =
        stateCell("gestureCenterFullscreenState", prefs.getBoolean("gesture_center_fullscreen", true))

    private val gestureBoostSwipeState =
        stateCell("gestureBoostSwipeState", prefs.getBoolean("gesture_boost_swipe", true))

    private val boostDefaultSpeedState =
        stateCell("boostDefaultSpeedState", prefs.getFloat("boost_default_speed", 3f))
    /** 打开每个视频时的默认播放倍速。 */

    private val defaultPlaybackSpeedState =
        stateCell("defaultPlaybackSpeedState", prefs.getFloat("default_playback_speed", 1f))

    private val showFullscreenTimeState = stateCell("showFullscreenTimeState", prefs.getBoolean("show_fullscreen_time", true))

    private val showFullscreenActionsState =
        stateCell("showFullscreenActionsState", prefs.getBoolean("show_fullscreen_actions", true))

    private val showCommentCountState =
        stateCell("showCommentCountState", prefs.getBoolean("show_comment_count", true))
    /** 进度条常显模式：0=关闭，1=仅半屏，2=仅全屏，3=永久。 */

    private val progressBarModeState = stateCell("progressBarModeState", prefs.getInt("progress_bar_mode", 0))

    private val sponsorSkipModeState = stateCell("sponsorSkipModeState", prefs.getInt("sponsor_skip_mode", 1))

    private val btLatencyMsState = stateCell("btLatencyMsState", prefs.getInt("bt_latency_ms", 100))

    /** 「我的」页底部「我的评论与弹幕」入口是否显示（默认显示）。 */

    override var qualityPreference: Int
        get() = qualityState.value.let { if (it in 1..63) 64 else it }
        set(value) {
            // 0 = 自动；其余最低 720P(64)，避免别处写进 480P 以下。
            val normalized = if (value in 1..63) 64 else value
            qualityState.value = normalized
            prefs.edit().putInt("quality", normalized).apply()
        }

    override var lowPower: Boolean
        get() = lowPowerState.value
        set(value) {
            lowPowerState.value = value
            prefs.edit().putBoolean("low_power", value).apply()
        }

    override var autoRotate: Boolean
        get() = autoRotateState.value
        set(value) {
            autoRotateState.value = value
            prefs.edit().putBoolean("auto_rotate", value).apply()
        }

    /** 视频页沉浸状态栏：视频画面延伸到状态栏下方，默认关闭。 */

    override var videoImmersiveStatusBar: Boolean
        get() = immersiveStatusState.value
        set(value) {
            immersiveStatusState.value = value
            prefs.edit().putBoolean("immersive_status", value).apply()
        }

    /** 是否已经问过一次通知权限（避免每次进视频都弹）。 */
    /** 评论正文字号倍率：默认 1.1（比原来略大），设置里 0.85~1.6 无极调节。 */

    override var backgroundPlay: Boolean
        get() = backgroundPlayState.value
        set(value) {
            backgroundPlayState.value = value
            prefs.edit().putBoolean("background_play", value).apply()
        }

    /** 显式离开视频页或关闭 PiP 时停止；不拦截切到桌面进入 PiP。 */

    override var stopOnVideoPageExit: Boolean
        get() = stopOnVideoPageExitState.value
        set(value) {
            stopOnVideoPageExitState.value = value
            prefs.edit().putBoolean("stop_on_video_page_exit", value).apply()
        }

    /** 退出视频后是否继续播放（显示唱片或进入画中画）。新停止选项开启时优先停止。 */

    override var showDiscAfterVideo: Boolean
        get() = showDiscAfterVideoState.value
        set(value) {
            showDiscAfterVideoState.value = value
            prefs.edit().putBoolean("show_disc_after_video", value).apply()
        }

    /** 退出视频后使用哪种控件：0 = 画中画，1 = 唱片（默认）。 */

    override var exitVideoControl: Int
        get() = exitVideoControlState.value
        set(value) {
            exitVideoControlState.value = value
            prefs.edit().putInt("exit_video_control", value).apply()
        }

    /** 唱片呼吸灯。 */

    override var discBreathEnabled: Boolean
        get() = discBreathEnabledState.value
        set(value) {
            discBreathEnabledState.value = value
            prefs.edit().putBoolean("disc_breath_enabled", value).apply()
        }

    /** 呼吸灯颜色是否跟随封面取色（关闭时默认白色）。 */

    override var discBreathCoverColor: Boolean
        get() = discBreathCoverColorState.value
        set(value) {
            discBreathCoverColorState.value = value
            prefs.edit().putBoolean("disc_breath_cover_color", value).apply()
        }

    override var autoPlay: Boolean
        get() = autoPlayState.value
        set(value) {
            autoPlayState.value = value
            prefs.edit().putBoolean("auto_play", value).apply()
        }

    override var autoPlayNext: Boolean
        get() = autoPlayNextState.value
        set(value) {
            autoPlayNextState.value = value
            prefs.edit().putBoolean("auto_play_next", value).apply()
        }

    override var loopMode: Int
        get() = loopModeState.value.coerceIn(0, 2)
        set(value) {
            val mode = value.coerceIn(0, 2)
            loopModeState.value = mode
            prefs.edit().putInt("loop_mode", mode).apply()
        }

    override var dynamicAutoPlay: Boolean
        get() = dynamicAutoPlayState.value
        set(value) {
            dynamicAutoPlayState.value = value
            prefs.edit().putBoolean("dynamic_auto_play", value).apply()
        }

    override var dynamicAutoPlayMuted: Boolean
        get() = dynamicAutoPlayMutedState.value
        set(value) {
            dynamicAutoPlayMutedState.value = value
            prefs.edit().putBoolean("dynamic_auto_play_muted", value).apply()
        }

    override var dynamicAutoPlayQuality: Int
        get() = dynamicAutoPlayQualityState.value
        set(value) {
            dynamicAutoPlayQualityState.value = value
            prefs.edit().putInt("dynamic_auto_play_quality", value).apply()
        }

    override var bluetoothLatencyEnabled: Boolean
        get() = btLatencyEnabledState.value
        set(value) {
            btLatencyEnabledState.value = value
            prefs.edit().putBoolean("bt_latency_enabled", value).apply()
        }

    /** 首页推荐：true = Web 端推荐，false = App 端推荐。 */

    override var loudnessEq: Boolean
        get() = loudnessEqState.value
        set(value) {
            loudnessEqState.value = value
            prefs.edit().putBoolean("loudness_eq", value).apply()
        }

    override var resumePlayback: Boolean
        get() = resumePlaybackState.value
        set(value) {
            resumePlaybackState.value = value
            prefs.edit().putBoolean("resume_playback", value).apply()
        }

    override var gestureBrightnessVolume: Boolean
        get() = gestureBrightnessVolumeState.value
        set(value) {
            gestureBrightnessVolumeState.value = value
            prefs.edit().putBoolean("gesture_brightness_volume", value).apply()
        }

    /** 在画面中间上下划动切换全屏。 */

    override var gestureCenterFullscreen: Boolean
        get() = gestureCenterFullscreenState.value
        set(value) {
            gestureCenterFullscreenState.value = value
            prefs.edit().putBoolean("gesture_center_fullscreen", value).apply()
        }

    /** 长按加速时左右滑动调节倍速。 */

    override var gestureBoostSwipe: Boolean
        get() = gestureBoostSwipeState.value
        set(value) {
            gestureBoostSwipeState.value = value
            prefs.edit().putBoolean("gesture_boost_swipe", value).apply()
        }

    /** 打开视频时的默认倍速，默认 1 倍。 */

    override var defaultPlaybackSpeed: Float
        get() = defaultPlaybackSpeedState.value.coerceIn(0.5f, 3f)
        set(value) {
            defaultPlaybackSpeedState.value = value.coerceIn(0.5f, 3f)
            prefs.edit()
                .putFloat("default_playback_speed", defaultPlaybackSpeedState.value)
                .apply()
        }

    /** 长按加速的默认倍率。 */

    override var boostDefaultSpeed: Float
        get() = boostDefaultSpeedState.value.coerceIn(1f, 5f)
        set(value) {
            boostDefaultSpeedState.value = value.coerceIn(1f, 5f)
            prefs.edit().putFloat("boost_default_speed", boostDefaultSpeedState.value).apply()
        }

    override var showFullscreenTime: Boolean
        get() = showFullscreenTimeState.value
        set(value) {
            showFullscreenTimeState.value = value
            prefs.edit().putBoolean("show_fullscreen_time", value).apply()
        }

    override var showFullscreenActions: Boolean
        get() = showFullscreenActionsState.value
        set(value) {
            showFullscreenActionsState.value = value
            prefs.edit().putBoolean("show_fullscreen_actions", value).apply()
        }

    /** 详情页「评论」标签是否附带评论数；关闭后只显示「评论」。 */
    override var showCommentCount: Boolean
        get() = showCommentCountState.value
        set(value) {
            showCommentCountState.value = value
            prefs.edit().putBoolean("show_comment_count", value).apply()
        }

    /** 进度条常显：0 关闭 / 1 仅半屏 / 2 仅全屏 / 3 永久。 */

    override var progressBarMode: Int
        get() = progressBarModeState.value.coerceIn(0, 3)
        set(value) {
            progressBarModeState.value = value.coerceIn(0, 3)
            prefs.edit().putInt("progress_bar_mode", progressBarModeState.value).apply()
        }

    override var sponsorSkipMode: Int
        get() = sponsorSkipModeState.value.coerceIn(0, 2)
        set(value) {
            sponsorSkipModeState.value = value.coerceIn(0, 2)
            prefs.edit().putInt("sponsor_skip_mode", sponsorSkipModeState.value).apply()
        }

    override var bluetoothLatencyMs: Int
        get() = btLatencyMsState.value
        set(value) {
            btLatencyMsState.value = value.coerceIn(50, 300)
            prefs.edit().putInt("bt_latency_ms", btLatencyMsState.value).apply()
        }

    override var decoderMode: Int
        get() = decoderState.value.coerceIn(0, 3)
        set(value) {
            decoderState.value = value.coerceIn(0, 3)
            prefs.edit().putInt("decoder_mode", decoderState.value).apply()
        }

    /** 首选视频编码：auto / avc / hevc / av1 / dvh1。 */

    override var codecPreference: String
        get() = codecPreferenceState.value
        set(value) {
            codecPreferenceState.value = value
            prefs.edit().putString("codec_preference", value).apply()
        }

    /** 视频 CDN 服务 id；auto 表示自动测速选择，base 表示不替换。 */

    override var cdnService: String
        get() = cdnServiceState.value
        set(value) {
            cdnServiceState.value = value
            prefs.edit().putString("cdn_service", value).apply()
        }

    /**
     * 搜索历史用 JSON 数组保存，保证顺序。
     * 旧版本用 StringSet 存，首次读取时兼容迁移一次。
     */

}
