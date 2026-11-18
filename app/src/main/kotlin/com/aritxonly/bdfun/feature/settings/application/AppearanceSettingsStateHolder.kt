package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.model.settings.allScrollTopPages
import com.aritxonly.bdfun.core.domain.*

internal class AppearanceSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), AppearanceSettings {
    private val feedRefreshRateState = stateCell("feedRefreshRateState",
        normalizeFeedRefreshRate(prefs.getInt("feed_refresh_rate", DEFAULT_FEED_REFRESH_RATE)))

    override var feedRefreshRate: Int
        get() = feedRefreshRateState.value
        set(value) {
            feedRefreshRateState.value = normalizeFeedRefreshRate(value)
            prefs.edit().putInt("feed_refresh_rate", feedRefreshRateState.value).apply()
        }

    private val dynamicColorState = stateCell("dynamicColorState", prefs.getBoolean("dynamic_color", false))

    private val tabClickRefreshState = stateCell("tabClickRefreshState",
        prefs.getBoolean("tab_click_refresh", TabClickPolicy.DEFAULT_REFRESH))
    override var tabClickRefresh: Boolean
        get() = tabClickRefreshState.value
        set(value) {
            tabClickRefreshState.value = value
            prefs.edit().putBoolean("tab_click_refresh", value).apply()
        }

    private val bottomBarRaiseState = stateCell("bottomBarRaiseState", prefs.getBoolean("bottom_bar_raise", false))

    private val darkModeState = stateCell("darkModeState", prefs.getInt("dark_mode", 0))
    /** 色彩风格：0 = AC 粉（B 站粉），1 = Hyper 蓝（Miuix 默认蓝）。 */

    private val colorStyleState = stateCell("colorStyleState", prefs.getInt("color_style", 0))
    /** 界面风格：0 官方，1 经典，2 HyperOS 4（底部搜索）。 */

    private val interfaceStyleState = stateCell("interfaceStyleState", prefs.getInt("interface_style", 2))
    /** 首页卡片样式：0 = 双排小卡，1 = 大卡（动态页视频卡同款）。 */

    private val homeChannelsState = stateCell("homeChannelsState",
        HomeChannelPolicy.normalize(prefs.getStringSet("home_channels", null) ?: HomeChannelPolicy.defaults))
    override var homeChannels: Set<String>
        get() = homeChannelsState.value
        set(value) {
            val normalized = HomeChannelPolicy.normalize(value)
            homeChannelsState.value = normalized
            prefs.edit().putStringSet("home_channels", normalized).apply()
        }

    private val homeCardStyleState = stateCell("homeCardStyleState", prefs.getInt("home_card_style", 0))

    private val shadowsState = stateCell("shadowsState", prefs.getBoolean("shadows", true))

    private val scrollTopEnabledState = stateCell("scrollTopEnabledState", prefs.getBoolean("scroll_top_enabled", true))
    /** 回到顶部按钮位置：0 = 左侧，1 = 右侧。 */

    private val scrollTopSideState = stateCell("scrollTopSideState", prefs.getInt("scroll_top_side", 1))
    /** 在哪些页面显示回到顶部按钮；没存过时默认全部页面。 */

    private val scrollTopPagesState = stateCell("scrollTopPagesState",
        prefs.getStringSet("scroll_top_pages", null) ?: allScrollTopPages,
    )

    init {
        // 新增直播/番剧/评论区三个页面：老用户已存过页面集合，不会自动包含它们，迁移一次补上。
        if (!prefs.getBoolean("scroll_top_pages_v3", false)) {
            val stored = prefs.getStringSet("scroll_top_pages", null)
            val merged = (stored ?: allScrollTopPages) + setOf("live", "pgc", "comment")
            scrollTopPagesState.value = merged
            prefs.edit()
                .putStringSet("scroll_top_pages", merged)
                .putBoolean("scroll_top_pages_v3", true)
                .apply()
        }
    }

    private val blurState = stateCell("blurState", prefs.getBoolean("blur", true))

    private val vibrationState = stateCell("vibrationState", prefs.getBoolean("vibration", true))

    private val commentFontScaleState =
        stateCell("commentFontScaleState", prefs.getFloat("comment_font_scale", 1.1f))

    /** 硬解模式：0 自动 / 1 硬件解码 / 2 软件解码 / 3 硬件解码（禁止回退）。 */

    private val splashWallpaperState = stateCell("splashWallpaperState", prefs.getString("splash_wallpaper", "").orEmpty())

    private val fontScaleState = stateCell("fontScaleState", prefs.getFloat("font_scale", 1f))

    /** 全局动效开关（关闭后所有手写补间/弹簧直接落位）。 */

    private val animationsEnabledState = stateCell("animationsEnabledState", prefs.getBoolean("animations_enabled", true))

    /** 预测性返回默认关闭：不跟手预览，返回仍保持原有离散提交。 */
    private val predictiveBackEnabledState = stateCell("predictiveBackEnabledState", prefs.getBoolean("predictive_back", false))

    private val refractionState = stateCell("refractionState", prefs.getBoolean("refraction", true))

    /** 玻璃折射（边缘弯折 + 色散），关闭后只保留模糊。 */

    override var dynamicColor: Boolean
        get() = dynamicColorState.value
        set(value) {
            dynamicColorState.value = value
            prefs.edit().putBoolean("dynamic_color", value).apply()
        }

    override var refractionEnabled: Boolean
        get() = refractionState.value
        set(value) {
            refractionState.value = value
            prefs.edit().putBoolean("refraction", value).apply()
        }

    override var bottomBarRaise: Boolean
        get() = bottomBarRaiseState.value
        set(value) {
            bottomBarRaiseState.value = value
            prefs.edit().putBoolean("bottom_bar_raise", value).apply()
        }

    override var animationsEnabled: Boolean
        get() = animationsEnabledState.value
        set(value) {
            animationsEnabledState.value = value
            prefs.edit().putBoolean("animations_enabled", value).apply()
        }

    override var predictiveBackEnabled: Boolean
        get() = predictiveBackEnabledState.value
        set(value) {
            predictiveBackEnabledState.value = value
            prefs.edit().putBoolean("predictive_back", value).apply()
        }

    override var darkMode: Int
        get() = darkModeState.value
        set(value) {
            darkModeState.value = value
            prefs.edit().putInt("dark_mode", value).apply()
        }

    override var colorStyle: Int
        get() = colorStyleState.value
        set(value) {
            colorStyleState.value = value.coerceIn(0, 1)
            prefs.edit().putInt("color_style", colorStyleState.value).apply()
        }

    /** 界面风格：0 官方，1 经典。默认官方，不影响原有 UI。 */

    override var interfaceStyle: Int
        get() = interfaceStyleState.value
        set(value) {
            interfaceStyleState.value = value.coerceIn(0, 2)
            prefs.edit().putInt("interface_style", interfaceStyleState.value).apply()
        }

    /** 首页卡片样式：0 双排，1 大卡。 */

    override var homeCardStyle: Int
        get() = homeCardStyleState.value
        set(value) {
            homeCardStyleState.value = value.coerceIn(0, 1)
            prefs.edit().putInt("home_card_style", homeCardStyleState.value).apply()
        }

    override var shadowsEnabled: Boolean
        get() = shadowsState.value
        set(value) {
            shadowsState.value = value
            prefs.edit().putBoolean("shadows", value).apply()
        }

    /** 列表页的「回到顶部」按钮，默认开启、默认右侧。 */

    override var scrollTopEnabled: Boolean
        get() = scrollTopEnabledState.value
        set(value) {
            scrollTopEnabledState.value = value
            prefs.edit().putBoolean("scroll_top_enabled", value).apply()
        }

    /** 0 = 左侧，1 = 右侧。 */

    override var scrollTopSide: Int
        get() = scrollTopSideState.value
        set(value) {
            scrollTopSideState.value = value.coerceIn(0, 1)
            prefs.edit().putInt("scroll_top_side", scrollTopSideState.value).apply()
        }

    /** 当前允许显示回到顶部按钮的页面集合。 */

    override var scrollTopPages: Set<String>
        get() = scrollTopPagesState.value
        set(value) {
            scrollTopPagesState.value = value
            prefs.edit().putStringSet("scroll_top_pages", value).apply()
        }

    override var blurEnabled: Boolean
        get() = blurState.value
        set(value) {
            blurState.value = value
            prefs.edit()
                .putBoolean("blur", value)
                .putBoolean("blur_pending", false)
                .apply()
        }

    /**
     * If the previous process died while blur was armed, disable blur before
     * the first frame so a driver/shader crash cannot brick the app.
     */

    override var vibrationEnabled: Boolean
        get() = vibrationState.value
        set(value) {
            vibrationState.value = value
            prefs.edit().putBoolean("vibration", value).apply()
        }

    override var commentFontScale: Float
        get() = commentFontScaleState.value
        set(value) {
            commentFontScaleState.value = value.coerceIn(0.85f, 1.6f)
            prefs.edit().putFloat("comment_font_scale", commentFontScaleState.value).apply()
        }

    override var splashWallpaper: String
        get() = splashWallpaperState.value
        set(value) {
            splashWallpaperState.value = value
            prefs.edit().putString("splash_wallpaper", value).apply()
        }

    override var fontScale: Float
        get() = fontScaleState.value
        set(value) {
            fontScaleState.value = value.coerceIn(0.8f, 1.2f)
            prefs.edit().putFloat("font_scale", fontScaleState.value).apply()
        }

    override fun isScrollTopPageEnabled(page: String): Boolean = page in scrollTopPagesState.value

    override fun setScrollTopPageEnabled(page: String, enabled: Boolean) {
        if (page !in allScrollTopPages) return
        scrollTopPages = if (enabled) scrollTopPages + page else scrollTopPages - page
    }

    override fun prepareBlurSession() {
        // CPU/PixelCopy blur is safe; no driver crash guard needed.
    }

    override fun markBlurSessionHealthy() {
        if (prefs.getBoolean("blur_pending", false)) {
            prefs.edit().putBoolean("blur_pending", false).apply()
        }
    }

}
