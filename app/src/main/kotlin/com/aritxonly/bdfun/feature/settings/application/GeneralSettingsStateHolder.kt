package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*

internal class GeneralSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), GeneralSettings {
    private val smartRefreshState = stateCell("smartRefreshState", prefs.getBoolean("smart_refresh", true))

    private val notifAskedState = stateCell("notifAskedState", prefs.getBoolean("notif_asked", false))

    private val recommendWebState = stateCell("recommendWebState", prefs.getBoolean("recommend_web", false))

    private val showViewerCountState = stateCell("showViewerCountState", prefs.getBoolean("show_viewer_count", true))

    private val showHotSearchState = stateCell("showHotSearchState", prefs.getBoolean("show_hot_search", true))

    private val showSearchHistoryState = stateCell("showSearchHistoryState", prefs.getBoolean("show_search_history", true))

    private val showSearchDiscoveryState = stateCell("showSearchDiscoveryState", prefs.getBoolean("show_search_discovery", true))

    private val autoCheckUpdateState = stateCell("autoCheckUpdateState", prefs.getBoolean("auto_check_update", true))

    private val termsAckedVersionState = stateCell("termsAckedVersionState",
        prefs.getString("terms_acked_version", "").orEmpty(),
    )

    private val lastSeenVersionState = stateCell("lastSeenVersionState",
        prefs.getString("last_seen_version", "").orEmpty(),
    )

    private val previewAckState = stateCell("previewAckState", prefs.getBoolean("preview_notice_acked", false))

    /** 首次启动的预览版说明是否已确认。 */

    override var previewNoticeAcked: Boolean
        get() = previewAckState.value
        set(value) {
            previewAckState.value = value
            prefs.edit().putBoolean("preview_notice_acked", value).apply()
        }

    override var smartRefresh: Boolean
        get() = smartRefreshState.value
        set(value) {
            smartRefreshState.value = value
            prefs.edit().putBoolean("smart_refresh", value).apply()
        }

    /** 未开启系统导航条时，把首页底栏和视频页回复框额外抬高。 */

    override var notifAsked: Boolean
        get() = notifAskedState.value
        set(value) {
            notifAskedState.value = value
            prefs.edit().putBoolean("notif_asked", value).apply()
        }

    override var recommendWeb: Boolean
        get() = recommendWebState.value
        set(value) {
            recommendWebState.value = value
            prefs.edit().putBoolean("recommend_web", value).apply()
        }

    override var showViewerCount: Boolean
        get() = showViewerCountState.value
        set(value) {
            showViewerCountState.value = value
            prefs.edit().putBoolean("show_viewer_count", value).apply()
        }

    override var showHotSearch: Boolean
        get() = showHotSearchState.value
        set(value) {
            showHotSearchState.value = value
            prefs.edit().putBoolean("show_hot_search", value).apply()
        }

    override var showSearchHistory: Boolean
        get() = showSearchHistoryState.value
        set(value) {
            showSearchHistoryState.value = value
            prefs.edit().putBoolean("show_search_history", value).apply()
        }

    override var showSearchDiscovery: Boolean
        get() = showSearchDiscoveryState.value
        set(value) {
            showSearchDiscoveryState.value = value
            prefs.edit().putBoolean("show_search_discovery", value).apply()
        }

    override var autoCheckUpdate: Boolean
        get() = autoCheckUpdateState.value
        set(value) {
            autoCheckUpdateState.value = value
            prefs.edit().putBoolean("auto_check_update", value).apply()
        }

    /** 新版开始页是否已经看过；装了这个版本的所有人都先看一次。 */

    override var onboardingAcked: Boolean
        get() = prefs.getBoolean("onboarding_acked_v4", false)
        set(value) { prefs.edit().putBoolean("onboarding_acked_v4", value).apply(); values.invalidate() }

    /** 已同意使用须知的版本；版本变化后重新提示一次。 */

    override var termsAckedVersion: String
        get() = termsAckedVersionState.value
        set(value) {
            termsAckedVersionState.value = value
            prefs.edit().putString("terms_acked_version", value).apply()
        }

    /** 上次启动时看到的版本，用于安装新版本后提示查看更新日志。 */

    override var lastSeenVersion: String
        get() = lastSeenVersionState.value
        set(value) {
            lastSeenVersionState.value = value
            prefs.edit().putString("last_seen_version", value).apply()
        }

    override val searchHistory: List<String>
        get() {
            val raw = prefs.getString("search_history_json", null)
            if (!raw.isNullOrBlank()) return codec.decode(raw)
            return prefs.getStringSet("search_history", emptySet()).orEmpty().toList()
        }


    override fun addSearchHistory(keyword: String) {
        if (keyword.isBlank()) return
        val next = (listOf(keyword) + searchHistory.filterNot { it == keyword }).take(12)
        prefs.edit()
            .putString("search_history_json", codec.encode(next))
            .remove("search_history")
            .apply()
        values.invalidate()
    }

    override fun clearSearchHistory() {
        prefs.edit().remove("search_history_json").remove("search_history").apply()
        values.invalidate()
    }

    override fun removeSearchHistory(keywords: Collection<String>) {
        if (keywords.isEmpty()) return
        val removed = keywords.toSet()
        val next = searchHistory.filterNot { it in removed }
        if (next.isEmpty()) clearSearchHistory()
        else {
            prefs.edit().putString("search_history_json", codec.encode(next)).remove("search_history").apply()
            values.invalidate()
        }
    }

    // 页面级搜索记录：独立键，不迁移旧的首页历史，也不会被首页清空操作影响。
    private fun scopedHistoryKey(scope: String) = "search_history_${scope}_json"

    override fun searchHistory(scope: String): List<String> {
        val raw = prefs.getString(scopedHistoryKey(scope), null)
        if (raw.isNullOrBlank()) return emptyList()
        return codec.decode(raw)
    }

    override fun addSearchHistory(scope: String, keyword: String) {
        if (keyword.isBlank()) return
        val next = (listOf(keyword) + searchHistory(scope).filterNot { it == keyword }).take(12)
        prefs.edit().putString(scopedHistoryKey(scope), codec.encode(next)).apply()
        values.invalidate()
    }

    override fun clearSearchHistory(scope: String) {
        prefs.edit().remove(scopedHistoryKey(scope)).apply()
        values.invalidate()
    }

}
