package com.aritxonly.bdfun.feature.settings.presentation

import com.aritxonly.bdfun.core.domain.SettingsRepository
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.teak.common.chrome.TeakMotion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

/** 逐字段 Compose 投影，只持有应用层控制器；Android 存储在启动层创建。
 * 每个契约字段显式投影，新增字段漏接时让编译器报错，不能回退成不参与观察的直接委托。
 */
open class SettingsBindings internal constructor(
    private val scope: CoroutineScope, private val controller: SettingsStateHolder,
) : SettingsRepository, AutoCloseable {
    val repository: SettingsRepository get() = controller
    override val appearance: com.aritxonly.bdfun.core.domain.AppearanceSettings get() = this
    override val playback: com.aritxonly.bdfun.core.domain.PlaybackSettings get() = this
    override val danmaku: com.aritxonly.bdfun.core.domain.DanmakuSettings get() = this
    override val message: com.aritxonly.bdfun.core.domain.MessageSettings get() = this
    override val general: com.aritxonly.bdfun.core.domain.GeneralSettings get() = this
    val state get() = controller.state
    private val projection = SettingsPresentationStateHolder(scope, state) { TeakMotion.enabled = controller.animationsEnabled }
    override var feedRefreshRate: Int by projection.property("feedRefreshRateState", { controller.feedRefreshRate }, { controller.feedRefreshRate = it })
    override var dynamicColor: Boolean by projection.property("dynamicColorState", { controller.dynamicColor }, { controller.dynamicColor = it })
    override var previewNoticeAcked: Boolean by projection.property("previewAckState", { controller.previewNoticeAcked }, { controller.previewNoticeAcked = it })
    override var refractionEnabled: Boolean by projection.property("refractionState", { controller.refractionEnabled }, { controller.refractionEnabled = it })
    override var smartRefresh: Boolean by projection.property("smartRefreshState", { controller.smartRefresh }, { controller.smartRefresh = it })
    override var tabClickRefresh: Boolean by projection.property("tabClickRefreshState", { controller.tabClickRefresh }, { controller.tabClickRefresh = it })
    override var bottomBarRaise: Boolean by projection.property("bottomBarRaiseState", { controller.bottomBarRaise }, { controller.bottomBarRaise = it })
    override var animationsEnabled: Boolean by projection.property("animationsEnabledState", { controller.animationsEnabled }, { controller.animationsEnabled = it })
    override var predictiveBackEnabled: Boolean by projection.property("predictiveBackEnabledState", { controller.predictiveBackEnabled }, { controller.predictiveBackEnabled = it })
    override var danmakuEnabled: Boolean by projection.property("danmakuState", { controller.danmakuEnabled }, { controller.danmakuEnabled = it })
    override var qualityPreference: Int by projection.property("qualityState", { controller.qualityPreference }, { controller.qualityPreference = it })
    override var lowPower: Boolean by projection.property("lowPowerState", { controller.lowPower }, { controller.lowPower = it })
    override var darkMode: Int by projection.property("darkModeState", { controller.darkMode }, { controller.darkMode = it })
    override var colorStyle: Int by projection.property("colorStyleState", { controller.colorStyle }, { controller.colorStyle = it })
    override var interfaceStyle: Int by projection.property("interfaceStyleState", { controller.interfaceStyle }, { controller.interfaceStyle = it })
    override var homeChannels: Set<String> by projection.property("homeChannelsState", { controller.homeChannels }, { controller.homeChannels = it })
    override var homeChannelOrder: List<String> by projection.property(
        "homeChannelOrderState",
        { controller.homeChannelOrder },
        { controller.homeChannelOrder = it },
    )
    override var homeCardStyle: Int by projection.property("homeCardStyleState", { controller.homeCardStyle }, { controller.homeCardStyle = it })
    override var shadowsEnabled: Boolean by projection.property("shadowsState", { controller.shadowsEnabled }, { controller.shadowsEnabled = it })
    override var scrollTopEnabled: Boolean by projection.property("scrollTopEnabledState", { controller.scrollTopEnabled }, { controller.scrollTopEnabled = it })
    override var scrollTopSide: Int by projection.property("scrollTopSideState", { controller.scrollTopSide }, { controller.scrollTopSide = it })
    override var scrollTopPages: Set<String> by projection.property("scrollTopPagesState", { controller.scrollTopPages }, { controller.scrollTopPages = it })
    override var blurEnabled: Boolean by projection.property("blurState", { controller.blurEnabled }, { controller.blurEnabled = it })
    override var vibrationEnabled: Boolean by projection.property("vibrationState", { controller.vibrationEnabled }, { controller.vibrationEnabled = it })
    override var danmakuScale: Float by projection.property("danmakuScaleState", { controller.danmakuScale }, { controller.danmakuScale = it })
    override var danmakuSpeed: Float by projection.property("danmakuSpeedState", { controller.danmakuSpeed }, { controller.danmakuSpeed = it })
    override var autoRotate: Boolean by projection.property("autoRotateState", { controller.autoRotate }, { controller.autoRotate = it })
    override var videoImmersiveStatusBar: Boolean by projection.property("immersiveStatusState", { controller.videoImmersiveStatusBar }, { controller.videoImmersiveStatusBar = it })
    override var commentFontScale: Float by projection.property("commentFontScaleState", { controller.commentFontScale }, { controller.commentFontScale = it })
    override var notifAsked: Boolean by projection.property("notifAskedState", { controller.notifAsked }, { controller.notifAsked = it })
    override var danmakuOn: Boolean by projection.property("dmOnState", { controller.danmakuOn }, { controller.danmakuOn = it })
    override var danmakuScaleInline: Float by projection.property("dmScaleInlineState", { controller.danmakuScaleInline }, { controller.danmakuScaleInline = it })
    override var danmakuScaleFullscreen: Float by projection.property("dmScaleFullscreenState", { controller.danmakuScaleFullscreen }, { controller.danmakuScaleFullscreen = it })
    override var danmakuArea: Float by projection.property("dmAreaState", { controller.danmakuArea }, { controller.danmakuArea = it })
    override var danmakuSpeedPref: Float by projection.property("dmSpeedState", { controller.danmakuSpeedPref }, { controller.danmakuSpeedPref = it })
    override var danmakuOpacity: Float by projection.property("dmAlphaState", { controller.danmakuOpacity }, { controller.danmakuOpacity = it })
    override var danmakuStroke: Float by projection.property("dmStrokeState", { controller.danmakuStroke }, { controller.danmakuStroke = it })
    override var danmakuLowRefresh: Boolean by projection.property("dmLowRefreshState", { controller.danmakuLowRefresh }, { controller.danmakuLowRefresh = it })
    override var danmakuPortraitGuard: Boolean by projection.property("dmPortraitGuardState", { controller.danmakuPortraitGuard }, { controller.danmakuPortraitGuard = it })
    override var danmakuDedupe: Boolean by projection.property("dmDedupeState", { controller.danmakuDedupe }, { controller.danmakuDedupe = it })
    override var danmakuSubtitleGuard: Boolean by projection.property("dmSubtitleGuardState", { controller.danmakuSubtitleGuard }, { controller.danmakuSubtitleGuard = it })
    override var danmakuBlockScroll: Boolean by projection.property("dmBlockScrollState", { controller.danmakuBlockScroll }, { controller.danmakuBlockScroll = it })
    override var danmakuBlockFixed: Boolean by projection.property("dmBlockFixedState", { controller.danmakuBlockFixed }, { controller.danmakuBlockFixed = it })
    override var danmakuBlockColor: Boolean by projection.property("dmBlockColorState", { controller.danmakuBlockColor }, { controller.danmakuBlockColor = it })
    override var danmakuBlockAdvanced: Boolean by projection.property("dmBlockAdvancedState", { controller.danmakuBlockAdvanced }, { controller.danmakuBlockAdvanced = it })
    override var danmakuBlockCount: Boolean by projection.property("dmBlockCountState", { controller.danmakuBlockCount }, { controller.danmakuBlockCount = it })
    override var danmakuKeywordFilter: Boolean by projection.property("dmKeywordFilterState", { controller.danmakuKeywordFilter }, { controller.danmakuKeywordFilter = it })
    override var danmakuKeywords: Set<String> by projection.property("dmKeywordsState", { controller.danmakuKeywords }, { controller.danmakuKeywords = it })
    override var backgroundPlay: Boolean by projection.property("backgroundPlayState", { controller.backgroundPlay }, { controller.backgroundPlay = it })
    override var stopOnVideoPageExit: Boolean by projection.property("stopOnVideoPageExitState", { controller.stopOnVideoPageExit }, { controller.stopOnVideoPageExit = it })
    override var showDiscAfterVideo: Boolean by projection.property("showDiscAfterVideoState", { controller.showDiscAfterVideo }, { controller.showDiscAfterVideo = it })
    override var exitVideoControl: Int by projection.property("exitVideoControlState", { controller.exitVideoControl }, { controller.exitVideoControl = it })
    override var discBreathEnabled: Boolean by projection.property("discBreathEnabledState", { controller.discBreathEnabled }, { controller.discBreathEnabled = it })
    override var discBreathCoverColor: Boolean by projection.property("discBreathCoverColorState", { controller.discBreathCoverColor }, { controller.discBreathCoverColor = it })
    override var autoPlay: Boolean by projection.property("autoPlayState", { controller.autoPlay }, { controller.autoPlay = it })
    override var autoPlayNext: Boolean by projection.property("autoPlayNextState", { controller.autoPlayNext }, { controller.autoPlayNext = it })
    override var dynamicAutoPlay: Boolean by projection.property("dynamicAutoPlayState", { controller.dynamicAutoPlay }, { controller.dynamicAutoPlay = it })
    override var dynamicAutoPlayMuted: Boolean by projection.property("dynamicAutoPlayMutedState", { controller.dynamicAutoPlayMuted }, { controller.dynamicAutoPlayMuted = it })
    override var dynamicAutoPlayQuality: Int by projection.property("dynamicAutoPlayQualityState", { controller.dynamicAutoPlayQuality }, { controller.dynamicAutoPlayQuality = it })
    override var bluetoothLatencyEnabled: Boolean by projection.property("btLatencyEnabledState", { controller.bluetoothLatencyEnabled }, { controller.bluetoothLatencyEnabled = it })
    override var recommendWeb: Boolean by projection.property("recommendWebState", { controller.recommendWeb }, { controller.recommendWeb = it })
    override var showIpLocation: Boolean by projection.property("showIpLocationState", { controller.showIpLocation }, { controller.showIpLocation = it })
    override var showViewerCount: Boolean by projection.property("showViewerCountState", { controller.showViewerCount }, { controller.showViewerCount = it })
    override var showHotSearch: Boolean by projection.property("showHotSearchState", { controller.showHotSearch }, { controller.showHotSearch = it })
    override var autoCheckUpdate: Boolean by projection.property("autoCheckUpdateState", { controller.autoCheckUpdate }, { controller.autoCheckUpdate = it })
    override var onboardingAcked: Boolean by projection.property("onboardingAcked", { controller.onboardingAcked }, { controller.onboardingAcked = it })
    override var termsAckedVersion: String by projection.property("termsAckedVersionState", { controller.termsAckedVersion }, { controller.termsAckedVersion = it })
    override var lastSeenVersion: String by projection.property("lastSeenVersionState", { controller.lastSeenVersion }, { controller.lastSeenVersion = it })
    override var splashWallpaper: String by projection.property("splashWallpaperState", { controller.splashWallpaper }, { controller.splashWallpaper = it })
    override var loudnessEq: Boolean by projection.property("loudnessEqState", { controller.loudnessEq }, { controller.loudnessEq = it })
    override var resumePlayback: Boolean by projection.property("resumePlaybackState", { controller.resumePlayback }, { controller.resumePlayback = it })
    override var gestureBrightnessVolume: Boolean by projection.property("gestureBrightnessVolumeState", { controller.gestureBrightnessVolume }, { controller.gestureBrightnessVolume = it })
    override var gestureCenterFullscreen: Boolean by projection.property("gestureCenterFullscreenState", { controller.gestureCenterFullscreen }, { controller.gestureCenterFullscreen = it })
    override var gestureBoostSwipe: Boolean by projection.property("gestureBoostSwipeState", { controller.gestureBoostSwipe }, { controller.gestureBoostSwipe = it })
    override var defaultPlaybackSpeed: Float by projection.property("defaultPlaybackSpeedState", { controller.defaultPlaybackSpeed }, { controller.defaultPlaybackSpeed = it })
    override var boostDefaultSpeed: Float by projection.property("boostDefaultSpeedState", { controller.boostDefaultSpeed }, { controller.boostDefaultSpeed = it })
    override var showFullscreenTime: Boolean by projection.property("showFullscreenTimeState", { controller.showFullscreenTime }, { controller.showFullscreenTime = it })
    override var showFullscreenActions: Boolean by projection.property("showFullscreenActionsState", { controller.showFullscreenActions }, { controller.showFullscreenActions = it })
    override var progressBarMode: Int by projection.property("progressBarModeState", { controller.progressBarMode }, { controller.progressBarMode = it })
    override var sponsorSkipMode: Int by projection.property("sponsorSkipModeState", { controller.sponsorSkipMode }, { controller.sponsorSkipMode = it })
    override var hotDanmakuEnabled: Boolean by projection.property("hotDanmakuState", { controller.hotDanmakuEnabled }, { controller.hotDanmakuEnabled = it })
    override var bluetoothLatencyMs: Int by projection.property("btLatencyMsState", { controller.bluetoothLatencyMs }, { controller.bluetoothLatencyMs = it })
    override var sentHistoryEntry: Boolean by projection.property("sentHistoryEntryState", { controller.sentHistoryEntry }, { controller.sentHistoryEntry = it })
    override var fontScale: Float by projection.property("fontScaleState", { controller.fontScale }, { controller.fontScale = it })
    override var adaptiveBuffering: Boolean by projection.property("adaptiveBufferingState", { controller.adaptiveBuffering }, { controller.adaptiveBuffering = it })
    override var decoderMode: Int by projection.property("decoderState", { controller.decoderMode }, { controller.decoderMode = it })
    override var codecPreference: String by projection.property("codecPreferenceState", { controller.codecPreference }, { controller.codecPreference = it })
    override var cdnService: String by projection.property("cdnServiceState", { controller.cdnService }, { controller.cdnService = it })
    override val searchHistory: List<String> by projection.readOnly("searchHistory") { controller.searchHistory }
    override fun isScrollTopPageEnabled(page: String): Boolean = controller.isScrollTopPageEnabled(page)
    override fun setScrollTopPageEnabled(page: String, enabled: Boolean) { controller.setScrollTopPageEnabled(page, enabled); projection.refresh() }
    override fun prepareBlurSession() { controller.prepareBlurSession(); projection.refresh() }
    override fun markBlurSessionHealthy() { controller.markBlurSessionHealthy(); projection.refresh() }
    override fun addSearchHistory(keyword: String) { controller.addSearchHistory(keyword); projection.refresh() }
    override fun clearSearchHistory() { controller.clearSearchHistory(); projection.refresh() }
    override fun close() { controller.flush(); scope.cancel() }
}
