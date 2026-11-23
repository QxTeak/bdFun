package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.model.settings.allScrollTopPages
import com.aritxonly.bdfun.core.domain.SettingsRepository
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class SettingsRoundTripTest(private val case: SettingCase<*>) {
    @Test fun defaultAndNormalizedValueSurviveRecreation() = case.verify()
    companion object {
        @JvmStatic @Parameterized.Parameters(name = "{0}")
        fun cases(): List<Array<Any>> {
            val cases = listOf(
            SettingCase("feedRefreshRate", 60, 120, { it.feedRefreshRate }, { s, v -> s.feedRefreshRate = v }),
            SettingCase("feedRefreshRate", 60, 0, { it.feedRefreshRate }, { s, v -> s.feedRefreshRate = v }),
            SettingCase("dynamicColor", false, true, { it.dynamicColor }, { s, v -> s.dynamicColor = v }),
            SettingCase("previewNoticeAcked", false, true, { it.previewNoticeAcked }, { s, v -> s.previewNoticeAcked = v }),
            SettingCase("refractionEnabled", true, false, { it.refractionEnabled }, { s, v -> s.refractionEnabled = v }),
            SettingCase("smartRefresh", true, false, { it.smartRefresh }, { s, v -> s.smartRefresh = v }),
            SettingCase("tabClickRefresh", true, false, { it.tabClickRefresh }, { s, v -> s.tabClickRefresh = v }),
            SettingCase("bottomBarRaise", false, true, { it.bottomBarRaise }, { s, v -> s.bottomBarRaise = v }),
            SettingCase("animationsEnabled", true, false, { it.animationsEnabled }, { s, v -> s.animationsEnabled = v }),
            SettingCase("predictiveBackEnabled", false, true, { it.predictiveBackEnabled }, { s, v -> s.predictiveBackEnabled = v }),
            SettingCase("danmakuEnabled", true, false, { it.danmakuEnabled }, { s, v -> s.danmakuEnabled = v }),
            SettingCase("adaptiveBuffering", false, true, { it.adaptiveBuffering }, { s, v -> s.adaptiveBuffering = v }),
            SettingCase("qualityPreference", 0, 80, { it.qualityPreference }, { s, v -> s.qualityPreference = v }),
            SettingCase("lowPower", false, true, { it.lowPower }, { s, v -> s.lowPower = v }),
            SettingCase("darkMode", 0, 1, { it.darkMode }, { s, v -> s.darkMode = v }),
            SettingCase("colorStyle", 0, 1, { it.colorStyle }, { s, v -> s.colorStyle = v }),
            SettingCase("interfaceStyle", 2, 0, { it.interfaceStyle }, { s, v -> s.interfaceStyle = v }),
            SettingCase("homeChannels", com.aritxonly.bdfun.core.domain.HomeChannelPolicy.defaults, setOf("recommend", "cinema"), { it.homeChannels }, { s, v -> s.homeChannels = v }),
            SettingCase("homeChannelOrder", com.aritxonly.bdfun.core.domain.HomeChannelPolicy.defaultOrder,
                com.aritxonly.bdfun.core.domain.HomeChannelPolicy.defaultOrder.reversed(),
                { it.homeChannelOrder }, { s, v -> s.homeChannelOrder = v }),
            SettingCase("dynamicFeedTabs", com.aritxonly.bdfun.core.domain.DynamicTabPolicy.defaults,
                setOf("all", "video"),
                { it.dynamicFeedTabs }, { s, v -> s.dynamicFeedTabs = v }),
            SettingCase("dynamicFeedTabOrder", com.aritxonly.bdfun.core.domain.DynamicTabPolicy.defaultOrder,
                com.aritxonly.bdfun.core.domain.DynamicTabPolicy.defaultOrder.reversed(),
                { it.dynamicFeedTabOrder }, { s, v -> s.dynamicFeedTabOrder = v }),
            SettingCase("homeCardStyle", 0, 1, { it.homeCardStyle }, { s, v -> s.homeCardStyle = v }),
            SettingCase("shadowsEnabled", true, false, { it.shadowsEnabled }, { s, v -> s.shadowsEnabled = v }),
            SettingCase("scrollTopEnabled", true, false, { it.scrollTopEnabled }, { s, v -> s.scrollTopEnabled = v }),
            SettingCase("scrollTopSide", 1, 0, { it.scrollTopSide }, { s, v -> s.scrollTopSide = v }),
            SettingCase("scrollTopPages", allScrollTopPages, setOf("home"), { it.scrollTopPages }, { s, v -> s.scrollTopPages = v }),
            SettingCase("blurEnabled", true, false, { it.blurEnabled }, { s, v -> s.blurEnabled = v }),
            SettingCase("vibrationEnabled", true, false, { it.vibrationEnabled }, { s, v -> s.vibrationEnabled = v }),
            SettingCase("danmakuScale", 1f, 1.2f, { it.danmakuScale }, { s, v -> s.danmakuScale = v }),
            SettingCase("danmakuSpeed", 1f, 1.2f, { it.danmakuSpeed }, { s, v -> s.danmakuSpeed = v }),
            SettingCase("autoRotate", true, false, { it.autoRotate }, { s, v -> s.autoRotate = v }),
            SettingCase("videoImmersiveStatusBar", false, true, { it.videoImmersiveStatusBar }, { s, v -> s.videoImmersiveStatusBar = v }),
            SettingCase("commentFontScale", 1.1f, 1.2f, { it.commentFontScale }, { s, v -> s.commentFontScale = v }),
            SettingCase("notifAsked", false, true, { it.notifAsked }, { s, v -> s.notifAsked = v }),
            SettingCase("danmakuOn", true, false, { it.danmakuOn }, { s, v -> s.danmakuOn = v }),
            SettingCase("danmakuScaleInline", 1f, 1.2f, { it.danmakuScaleInline }, { s, v -> s.danmakuScaleInline = v }),
            SettingCase("danmakuScaleFullscreen", 1f, 1.2f, { it.danmakuScaleFullscreen }, { s, v -> s.danmakuScaleFullscreen = v }),
            SettingCase("danmakuArea", 1f, 0.7f, { it.danmakuArea }, { s, v -> s.danmakuArea = v }),
            SettingCase("danmakuSpeedPref", 1f, 1.2f, { it.danmakuSpeedPref }, { s, v -> s.danmakuSpeedPref = v }),
            SettingCase("danmakuOpacity", 1f, 0.7f, { it.danmakuOpacity }, { s, v -> s.danmakuOpacity = v }),
            SettingCase("danmakuStroke", 1.5f, 2f, { it.danmakuStroke }, { s, v -> s.danmakuStroke = v }),
            SettingCase("danmakuLowRefresh", true, false, { it.danmakuLowRefresh }, { s, v -> s.danmakuLowRefresh = v }),
            SettingCase("danmakuPortraitGuard", false, true, { it.danmakuPortraitGuard }, { s, v -> s.danmakuPortraitGuard = v }),
            SettingCase("danmakuDedupe", false, true, { it.danmakuDedupe }, { s, v -> s.danmakuDedupe = v }),
            SettingCase("danmakuSubtitleGuard", false, true, { it.danmakuSubtitleGuard }, { s, v -> s.danmakuSubtitleGuard = v }),
            SettingCase("danmakuBlockScroll", false, true, { it.danmakuBlockScroll }, { s, v -> s.danmakuBlockScroll = v }),
            SettingCase("danmakuBlockFixed", false, true, { it.danmakuBlockFixed }, { s, v -> s.danmakuBlockFixed = v }),
            SettingCase("danmakuBlockColor", false, true, { it.danmakuBlockColor }, { s, v -> s.danmakuBlockColor = v }),
            SettingCase("danmakuBlockAdvanced", false, true, { it.danmakuBlockAdvanced }, { s, v -> s.danmakuBlockAdvanced = v }),
            SettingCase("danmakuBlockCount", false, true, { it.danmakuBlockCount }, { s, v -> s.danmakuBlockCount = v }),
            SettingCase("danmakuKeywordFilter", false, true, { it.danmakuKeywordFilter }, { s, v -> s.danmakuKeywordFilter = v }),
            SettingCase("danmakuKeywords", emptySet<String>(), setOf("过滤词"), { it.danmakuKeywords }, { s, v -> s.danmakuKeywords = v }),
            SettingCase("backgroundPlay", true, false, { it.backgroundPlay }, { s, v -> s.backgroundPlay = v }),
            SettingCase("stopOnVideoPageExit", false, true, { it.stopOnVideoPageExit }, { s, v -> s.stopOnVideoPageExit = v }),
            SettingCase("showDiscAfterVideo", true, false, { it.showDiscAfterVideo }, { s, v -> s.showDiscAfterVideo = v }),
            SettingCase("exitVideoControl", 1, 0, { it.exitVideoControl }, { s, v -> s.exitVideoControl = v }),
            SettingCase("discBreathEnabled", false, true, { it.discBreathEnabled }, { s, v -> s.discBreathEnabled = v }),
            SettingCase("discBreathCoverColor", false, true, { it.discBreathCoverColor }, { s, v -> s.discBreathCoverColor = v }),
            SettingCase("autoPlay", true, false, { it.autoPlay }, { s, v -> s.autoPlay = v }),
            SettingCase("autoPlayNext", true, false, { it.autoPlayNext }, { s, v -> s.autoPlayNext = v }),
            SettingCase("dynamicAutoPlay", true, false, { it.dynamicAutoPlay }, { s, v -> s.dynamicAutoPlay = v }),
            SettingCase("dynamicAutoPlayMuted", true, false, { it.dynamicAutoPlayMuted }, { s, v -> s.dynamicAutoPlayMuted = v }),
            SettingCase("dynamicAutoPlayQuality", 64, 80, { it.dynamicAutoPlayQuality }, { s, v -> s.dynamicAutoPlayQuality = v }),
            SettingCase("bluetoothLatencyEnabled", false, true, { it.bluetoothLatencyEnabled }, { s, v -> s.bluetoothLatencyEnabled = v }),
            SettingCase("recommendWeb", false, true, { it.recommendWeb }, { s, v -> s.recommendWeb = v }),
            SettingCase("showIpLocation", true, false, { it.showIpLocation }, { s, v -> s.showIpLocation = v }),
            SettingCase("showViewerCount", true, false, { it.showViewerCount }, { s, v -> s.showViewerCount = v }),
            SettingCase("showHotSearch", true, false, { it.showHotSearch }, { s, v -> s.showHotSearch = v }),
            SettingCase("showSearchHistory", true, false, { it.showSearchHistory }, { s, v -> s.showSearchHistory = v }),
            SettingCase("showSearchDiscovery", true, false, { it.showSearchDiscovery }, { s, v -> s.showSearchDiscovery = v }),
            SettingCase("autoCheckUpdate", true, false, { it.autoCheckUpdate }, { s, v -> s.autoCheckUpdate = v }),
            SettingCase("onboardingAcked", false, true, { it.onboardingAcked }, { s, v -> s.onboardingAcked = v }),
            SettingCase("termsAckedVersion", "", "saved-value", { it.termsAckedVersion }, { s, v -> s.termsAckedVersion = v }),
            SettingCase("lastSeenVersion", "", "saved-value", { it.lastSeenVersion }, { s, v -> s.lastSeenVersion = v }),
            SettingCase("splashWallpaper", "", "saved-value", { it.splashWallpaper }, { s, v -> s.splashWallpaper = v }),
            SettingCase("loudnessEq", false, true, { it.loudnessEq }, { s, v -> s.loudnessEq = v }),
            SettingCase("resumePlayback", true, false, { it.resumePlayback }, { s, v -> s.resumePlayback = v }),
            SettingCase("gestureBrightnessVolume", true, false, { it.gestureBrightnessVolume }, { s, v -> s.gestureBrightnessVolume = v }),
            SettingCase("gestureCenterFullscreen", true, false, { it.gestureCenterFullscreen }, { s, v -> s.gestureCenterFullscreen = v }),
            SettingCase("gestureBoostSwipe", true, false, { it.gestureBoostSwipe }, { s, v -> s.gestureBoostSwipe = v }),
            SettingCase("defaultPlaybackSpeed", 1f, 1.5f, { it.defaultPlaybackSpeed }, { s, v -> s.defaultPlaybackSpeed = v }),
            SettingCase("boostDefaultSpeed", 3f, 4f, { it.boostDefaultSpeed }, { s, v -> s.boostDefaultSpeed = v }),
            SettingCase("showFullscreenTime", true, false, { it.showFullscreenTime }, { s, v -> s.showFullscreenTime = v }),
            SettingCase("showFullscreenActions", true, false, { it.showFullscreenActions }, { s, v -> s.showFullscreenActions = v }),
            SettingCase("progressBarMode", 0, 1, { it.progressBarMode }, { s, v -> s.progressBarMode = v }),
            SettingCase("sponsorSkipMode", 1, 2, { it.sponsorSkipMode }, { s, v -> s.sponsorSkipMode = v }),
            SettingCase("hotDanmakuEnabled", true, false, { it.hotDanmakuEnabled }, { s, v -> s.hotDanmakuEnabled = v }),
            SettingCase("videoInteraction", true, false, { it.videoInteraction }, { s, v -> s.videoInteraction = v }),
            SettingCase("bluetoothLatencyMs", 100, 0, { it.bluetoothLatencyMs }, { s, v -> s.bluetoothLatencyMs = v }),
            SettingCase("sentHistoryEntry", true, false, { it.sentHistoryEntry }, { s, v -> s.sentHistoryEntry = v }),
            SettingCase("fontScale", 1f, 1.2f, { it.fontScale }, { s, v -> s.fontScale = v }),
            SettingCase("decoderMode", 0, 1, { it.decoderMode }, { s, v -> s.decoderMode = v }),
            SettingCase("codecPreference", "auto", "avc", { it.codecPreference }, { s, v -> s.codecPreference = v }),
            SettingCase("cdnService", "base", "auto", { it.cdnService }, { s, v -> s.cdnService = v }),
            )
            val setters = SettingsRepository::class.java.methods
                .filter { it.name.startsWith("set") && it.parameterCount == 1 }
                .map { it.name.removePrefix("set").replaceFirstChar { c -> c.lowercaseChar() } }.toSet()
            assertEquals("每个可写设置都必须有默认值和往返测试", setters, cases.map { it.toString() }.toSet())
            return cases.map { arrayOf<Any>(it) }
        }
    }
}

class SettingCase<T>(
    private val name: String, private val default: T, private val changed: T,
    private val read: (SettingsRepository) -> T, private val write: (SettingsRepository, T) -> Unit,
) {
    override fun toString() = name
    fun verify() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val prefs = MemorySettingsPreferences()
            val first = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals("默认值：$name", default, read(first))
            write(first, changed)
            first.flush()
            val expected = read(first)
            assertNotEquals("写入必须生效：$name", default, expected)
            val restored = SettingsStateHolder.create(prefs, TestHistoryCodec, scope)
            assertEquals("往返：$name", expected, read(restored))
        } finally { scope.cancel() }
    }
}
