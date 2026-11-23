package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.domain.*

internal class MessageSettingsStateHolder(
    prefs: SettingsPreferences,
    values: SettingsValuesStateHolder,
    floats: FloatSettingsWriter,
    codec: SearchHistoryCodec,
) : SettingsGroupStateHolder(prefs, values, floats, codec), MessageSettings {
    private val showIpLocationState = stateCell("showIpLocationState", prefs.getBoolean("show_ip_location", true))

    private val sentHistoryEntryState =
        stateCell("sentHistoryEntryState", prefs.getBoolean("sent_history_entry", true))

    /** 默认关闭：屏蔽会改变评论区内容，未主动开启前必须和上一版表现一致。 */
    private val commentAiBlockEnabledState =
        stateCell("commentAiBlockEnabledState", prefs.getBoolean("comment_ai_block", false))

    private val commentAiBlockStrengthState =
        stateCell("commentAiBlockStrengthState", prefs.getInt("comment_ai_block_strength", STRENGTH_STANDARD))

    private val commentAiBlockReplyModeState =
        stateCell("commentAiBlockReplyModeState", prefs.getInt("comment_ai_block_reply_mode", REPLY_MODE_ONLY))

    /** 全局字号倍率（基于当前默认字号，100% 即完全不缩放）。 */

    override var showIpLocation: Boolean
        get() = showIpLocationState.value
        set(value) {
            showIpLocationState.value = value
            prefs.edit().putBoolean("show_ip_location", value).apply()
        }

    override var sentHistoryEntry: Boolean
        get() = sentHistoryEntryState.value
        set(value) {
            sentHistoryEntryState.value = value
            prefs.edit().putBoolean("sent_history_entry", value).apply()
        }

    override var commentAiBlockEnabled: Boolean
        get() = commentAiBlockEnabledState.value
        set(value) {
            commentAiBlockEnabledState.value = value
            prefs.edit().putBoolean("comment_ai_block", value).apply()
        }

    override var commentAiBlockStrength: Int
        get() = commentAiBlockStrengthState.value
        set(value) {
            commentAiBlockStrengthState.value = value
            prefs.edit().putInt("comment_ai_block_strength", value).apply()
        }

    override var commentAiBlockReplyMode: Int
        get() = commentAiBlockReplyModeState.value
        set(value) {
            commentAiBlockReplyModeState.value = value
            prefs.edit().putInt("comment_ai_block_reply_mode", value).apply()
        }

}
