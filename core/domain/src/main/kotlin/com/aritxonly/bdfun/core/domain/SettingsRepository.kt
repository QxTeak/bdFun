package com.aritxonly.bdfun.core.domain

interface AppearanceSettings {
    var feedRefreshRate: Int
    var dynamicColor: Boolean
    var refractionEnabled: Boolean
    var tabClickRefresh: Boolean
    var bottomBarRaise: Boolean
    var animationsEnabled: Boolean
    /** 预测性返回：手指返回手势过程中是否预览上一页；关闭时松手才提交。 */
    var predictiveBackEnabled: Boolean
    var darkMode: Int
    var colorStyle: Int
    var interfaceStyle: Int
    var homeChannels: Set<String>
    /** 首页频道 Tab 的显示顺序；只影响顺序，是否显示仍由 [homeChannels] 决定。 */
    var homeChannelOrder: List<String>
    var homeCardStyle: Int
    var shadowsEnabled: Boolean
    var scrollTopEnabled: Boolean
    var scrollTopSide: Int
    var scrollTopPages: Set<String>
    var blurEnabled: Boolean
    /** 全局触感/震动总开关；关闭后 App 内不再触发任何震动。 */
    var vibrationEnabled: Boolean
    var commentFontScale: Float
    var splashWallpaper: String
    var fontScale: Float
    fun isScrollTopPageEnabled(page: String): Boolean
    fun setScrollTopPageEnabled(page: String, enabled: Boolean)
    fun prepareBlurSession()
    fun markBlurSessionHealthy()
}

interface PlaybackSettings {
    var adaptiveBuffering: Boolean
    var qualityPreference: Int
    var lowPower: Boolean
    var autoRotate: Boolean
    var videoImmersiveStatusBar: Boolean
    var backgroundPlay: Boolean
    var stopOnVideoPageExit: Boolean
    var showDiscAfterVideo: Boolean
    var exitVideoControl: Int
    var discBreathEnabled: Boolean
    var discBreathCoverColor: Boolean
    var autoPlay: Boolean
    var autoPlayNext: Boolean
    var dynamicAutoPlay: Boolean
    var dynamicAutoPlayMuted: Boolean
    var dynamicAutoPlayQuality: Int
    var bluetoothLatencyEnabled: Boolean
    var loudnessEq: Boolean
    var resumePlayback: Boolean
    var gestureBrightnessVolume: Boolean
    var gestureCenterFullscreen: Boolean
    var gestureBoostSwipe: Boolean
    var defaultPlaybackSpeed: Float
    var boostDefaultSpeed: Float
    var showFullscreenTime: Boolean
    var showFullscreenActions: Boolean
    var progressBarMode: Int
    /** 空降助手：0 关闭 / 1 提示 / 2 自动跳过。 */
    var sponsorSkipMode: Int
    var bluetoothLatencyMs: Int
    var decoderMode: Int
    var codecPreference: String
    var cdnService: String
}

interface DanmakuSettings {
    var danmakuEnabled: Boolean
    var danmakuScale: Float
    var danmakuSpeed: Float
    var danmakuOn: Boolean
    var danmakuScaleInline: Float
    var danmakuScaleFullscreen: Float
    var danmakuArea: Float
    var danmakuSpeedPref: Float
    var danmakuOpacity: Float
    var danmakuStroke: Float
    var danmakuLowRefresh: Boolean
    var danmakuPortraitGuard: Boolean
    var danmakuDedupe: Boolean
    var danmakuSubtitleGuard: Boolean
    var danmakuBlockScroll: Boolean
    var danmakuBlockFixed: Boolean
    var danmakuBlockColor: Boolean
    var danmakuBlockAdvanced: Boolean
    var danmakuBlockCount: Boolean
    var danmakuKeywordFilter: Boolean
    var danmakuKeywords: Set<String>
    var hotDanmakuEnabled: Boolean
}

interface MessageSettings {
    var showIpLocation: Boolean
    var sentHistoryEntry: Boolean
}

interface GeneralSettings {
    var previewNoticeAcked: Boolean
    var smartRefresh: Boolean
    var notifAsked: Boolean
    var recommendWeb: Boolean
    var showViewerCount: Boolean
    var showHotSearch: Boolean
    var autoCheckUpdate: Boolean
    var onboardingAcked: Boolean
    var termsAckedVersion: String
    var lastSeenVersion: String
    val searchHistory: List<String>
    fun addSearchHistory(keyword: String)
    fun clearSearchHistory()
}

interface SettingsRepository : AppearanceSettings, PlaybackSettings, DanmakuSettings, MessageSettings, GeneralSettings {
    val appearance: AppearanceSettings
    val playback: PlaybackSettings
    val danmaku: DanmakuSettings
    val message: MessageSettings
    val general: GeneralSettings
}
