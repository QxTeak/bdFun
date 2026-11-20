package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*

internal class DanmakuSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), DanmakuSettings {
    private val danmakuState = stateCell("danmakuState", prefs.getBoolean("danmaku", true))

    private val danmakuScaleState = stateCell("danmakuScaleState", prefs.getFloat("danmaku_scale", 1f))

    private val danmakuSpeedState = stateCell("danmakuSpeedState", prefs.getFloat("danmaku_speed", 1f))

    private val dmOnState = stateCell("dmOnState", prefs.getBoolean("dm_on", true))

    private val dmScaleInlineState = stateCell("dmScaleInlineState", prefs.getFloat("dm_scale_inline", 1f))

    private val dmScaleFullscreenState = stateCell("dmScaleFullscreenState", prefs.getFloat("dm_scale_fullscreen", 1f))

    private val dmAreaState = stateCell("dmAreaState", prefs.getFloat("dm_area", 1f))

    private val dmSpeedState = stateCell("dmSpeedState", prefs.getFloat("dm_speed", 1f))

    private val dmAlphaState = stateCell("dmAlphaState", prefs.getFloat("dm_alpha", 1f))
    /** 降低弹幕刷新率（默认开）：限制到 60 帧。 */

    private val dmLowRefreshState = stateCell("dmLowRefreshState", prefs.getBoolean("dm_low_refresh", true))
    /** 弹幕黑色描边宽度（dp），0 表示不描边。 */

    private val dmStrokeState = stateCell("dmStrokeState", prefs.getFloat("dm_stroke", 1.5f))

    private val dmPortraitGuardState = stateCell("dmPortraitGuardState", prefs.getBoolean("dm_portrait_guard", false))

    private val dmDedupeState = stateCell("dmDedupeState", prefs.getBoolean("dm_dedupe", false))

    private val dmSubtitleGuardState = stateCell("dmSubtitleGuardState", prefs.getBoolean("dm_subtitle_guard", false))

    private val dmBlockScrollState = stateCell("dmBlockScrollState", prefs.getBoolean("dm_block_scroll", false))

    private val dmBlockFixedState = stateCell("dmBlockFixedState", prefs.getBoolean("dm_block_fixed", false))

    private val dmBlockColorState = stateCell("dmBlockColorState", prefs.getBoolean("dm_block_color", false))

    private val dmBlockAdvancedState = stateCell("dmBlockAdvancedState", prefs.getBoolean("dm_block_advanced", false))

    private val dmBlockCountState = stateCell("dmBlockCountState", prefs.getBoolean("dm_block_count", false))

    private val dmKeywordFilterState = stateCell("dmKeywordFilterState", prefs.getBoolean("dm_keyword_filter", false))

    private val dmKeywordsState = stateCell("dmKeywordsState",
        prefs.getStringSet("dm_keywords", emptySet()).orEmpty().toSet(),
    )

    private val hotDanmakuState = stateCell("hotDanmakuState", prefs.getBoolean("hot_danmaku", true))
    private val videoInteractionState = stateCell(
        "videoInteractionState", prefs.getBoolean("video_interaction", true),
    )

    override var danmakuEnabled: Boolean
        get() = danmakuState.value
        set(value) {
            danmakuState.value = value
            prefs.edit().putBoolean("danmaku", value).apply()
        }

    override var danmakuScale: Float
        get() = danmakuScaleState.value
        set(value) {
            danmakuScaleState.value = value.coerceIn(0.75f, 1.6f)
            prefs.edit().putFloat("danmaku_scale", danmakuScaleState.value).apply()
        }

    override var danmakuSpeed: Float
        get() = danmakuSpeedState.value
        set(value) {
            danmakuSpeedState.value = value.coerceIn(0.6f, 1.8f)
            prefs.edit().putFloat("danmaku_speed", danmakuSpeedState.value).apply()
        }

    override var danmakuOn: Boolean
        get() = dmOnState.value
        set(value) {
            dmOnState.value = value
            prefs.edit().putBoolean("dm_on", value).apply()
        }

    override var danmakuScaleInline: Float
        get() = dmScaleInlineState.value
        set(value) {
            dmScaleInlineState.value = value.coerceIn(0.6f, 2f)
            persistFloatEventually("dm_scale_inline", dmScaleInlineState.value)
        }

    override var danmakuScaleFullscreen: Float
        get() = dmScaleFullscreenState.value
        set(value) {
            dmScaleFullscreenState.value = value.coerceIn(0.6f, 2f)
            persistFloatEventually("dm_scale_fullscreen", dmScaleFullscreenState.value)
        }

    override var danmakuArea: Float
        get() = dmAreaState.value
        set(value) {
            dmAreaState.value = value.coerceIn(0.15f, 1f)
            persistFloatEventually("dm_area", dmAreaState.value)
        }

    override var danmakuSpeedPref: Float
        get() = dmSpeedState.value
        set(value) {
            dmSpeedState.value = value.coerceIn(0.5f, 2f)
            persistFloatEventually("dm_speed", dmSpeedState.value)
        }

    override var danmakuOpacity: Float
        get() = dmAlphaState.value
        set(value) {
            dmAlphaState.value = value.coerceIn(0.2f, 1f)
            persistFloatEventually("dm_alpha", dmAlphaState.value)
        }

    /** 弹幕描边宽度，单位 dp；无极调节。 */

    override var danmakuStroke: Float
        get() = dmStrokeState.value
        set(value) {
            dmStrokeState.value = value.coerceIn(0f, 4f)
            persistFloatEventually("dm_stroke", dmStrokeState.value)
        }

    /** 降低弹幕刷新率：开启后弹幕最多 60 帧，关闭后跟屏幕刷新率。 */

    override var danmakuLowRefresh: Boolean
        get() = dmLowRefreshState.value
        set(value) {
            dmLowRefreshState.value = value
            prefs.edit().putBoolean("dm_low_refresh", value).apply()
        }

    /** 人像防挡：弹幕避开画面中间的人像区域。 */

    override var danmakuPortraitGuard: Boolean
        get() = dmPortraitGuardState.value
        set(value) {
            dmPortraitGuardState.value = value
            prefs.edit().putBoolean("dm_portrait_guard", value).apply()
        }

    override var danmakuDedupe: Boolean
        get() = dmDedupeState.value
        set(value) {
            dmDedupeState.value = value
            prefs.edit().putBoolean("dm_dedupe", value).apply()
        }

    override var danmakuSubtitleGuard: Boolean
        get() = dmSubtitleGuardState.value
        set(value) {
            dmSubtitleGuardState.value = value
            prefs.edit().putBoolean("dm_subtitle_guard", value).apply()
        }

    override var danmakuBlockScroll: Boolean
        get() = dmBlockScrollState.value
        set(value) {
            dmBlockScrollState.value = value
            prefs.edit().putBoolean("dm_block_scroll", value).apply()
        }

    override var danmakuBlockFixed: Boolean
        get() = dmBlockFixedState.value
        set(value) {
            dmBlockFixedState.value = value
            prefs.edit().putBoolean("dm_block_fixed", value).apply()
        }

    override var danmakuBlockColor: Boolean
        get() = dmBlockColorState.value
        set(value) {
            dmBlockColorState.value = value
            prefs.edit().putBoolean("dm_block_color", value).apply()
        }

    override var danmakuBlockAdvanced: Boolean
        get() = dmBlockAdvancedState.value
        set(value) {
            dmBlockAdvancedState.value = value
            prefs.edit().putBoolean("dm_block_advanced", value).apply()
        }

    override var danmakuBlockCount: Boolean
        get() = dmBlockCountState.value
        set(value) {
            dmBlockCountState.value = value
            prefs.edit().putBoolean("dm_block_count", value).apply()
        }

    override var danmakuKeywordFilter: Boolean
        get() = dmKeywordFilterState.value
        set(value) {
            dmKeywordFilterState.value = value
            prefs.edit().putBoolean("dm_keyword_filter", value).apply()
        }

    override var danmakuKeywords: Set<String>
        get() = dmKeywordsState.value
        set(value) {
            dmKeywordsState.value = value
            prefs.edit().putStringSet("dm_keywords", value).apply()
        }

    override var hotDanmakuEnabled: Boolean
        get() = hotDanmakuState.value
        set(value) {
            hotDanmakuState.value = value
            prefs.edit().putBoolean("hot_danmaku", value).apply()
        }

    override var videoInteraction: Boolean
        get() = videoInteractionState.value
        set(value) {
            videoInteractionState.value = value
            prefs.edit().putBoolean("video_interaction", value).apply()
        }

}
