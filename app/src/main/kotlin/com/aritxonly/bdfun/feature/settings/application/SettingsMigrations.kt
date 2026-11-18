package com.aritxonly.bdfun.feature.settings.application

import com.aritxonly.bdfun.core.model.settings.AUTO_CDN_ID
import com.aritxonly.bdfun.core.domain.SettingsPreferences

internal fun migrateSettings0(prefs: SettingsPreferences) {
        // main 已将独立入口合并到 HyperOS 4，兼容清理旧开关键。
        if (prefs.contains("bottom_search")) prefs.edit().remove("bottom_search").apply()
        // 3.6 起首页 Web 推荐默认关闭；老版本把默认值 true 写进了偏好，升级时一次性改回 false。
        if (!prefs.getBoolean("recommend_web_migrated_36", false)) {
            prefs.edit()
                .putBoolean("recommend_web", false)
                .putBoolean("recommend_web_migrated_36", true)
                .apply()
        }
        // 3.7 起默认清晰度为「自动」；只把仍是旧默认 1080P(80) 的用户迁到自动。
        if (!prefs.getBoolean("quality_migrated_37", false)) {
            val editor = prefs.edit().putBoolean("quality_migrated_37", true)
            if (prefs.getInt("quality", 80) == 80) editor.putInt("quality", 0)
            editor.apply()
        }
        // preview-4.3：不再提供 720P 以下的选项，老用户存过的 480P(32) 及更低档迁到 720P。
        if (!prefs.getBoolean("quality_migrated_43", false)) {
            val editor = prefs.edit().putBoolean("quality_migrated_43", true)
            val stored = prefs.getInt("quality", 0)
            if (stored in 1..63) editor.putInt("quality", 64)
            editor.apply()
        }
        // 3.7 起动态视频自动播放默认开启；没动过开关的用户直接置为开。
        if (!prefs.contains("dynamic_auto_play")) {
            prefs.edit().putBoolean("dynamic_auto_play", true).apply()
        }
        // preview-3.9：解码器改为 4 档硬解模式，旧 7 档数值一次性映射过来。
        if (!prefs.getBoolean("decoder_mode_v2", false)) {
            val mapped = when (prefs.getInt("decoder_mode", 0)) {
                2 -> 1 // 旧「仅硬件」
                3, 5 -> 2 // 旧「仅软件」「软件优先」
                6 -> 3 // 旧「禁回退」
                else -> 0 // 自动 / 默认 / 硬件优先
            }
            prefs.edit()
                .putInt("decoder_mode", mapped)
                .putBoolean("decoder_mode_v2", true)
                .apply()
        }
        // preview-3.10.0：把旧版本遗留的「首页使用 Web 推荐」迁回 App 推荐一次。
        if (!prefs.getBoolean("recommend_app_v2", false)) {
            prefs.edit()
                .putBoolean("recommend_web", false)
                .putBoolean("recommend_app_v2", true)
                .apply()
        }
        // preview-3.9.5：重新提供「自动」选项；新安装默认自动，已有手动选择保持不变。
        if (!prefs.getBoolean("cdn_auto_v2", false)) {
            if (!prefs.contains("cdn_service")) {
                prefs.edit().putString("cdn_service", AUTO_CDN_ID).apply()
            }
            prefs.edit().putBoolean("cdn_auto_v2", true).apply()
        }
    }

internal fun migrateSettings1(prefs: SettingsPreferences) {
        if (!prefs.getBoolean("blur_cpu_v1", false)) {
            prefs.edit()
                .putBoolean("blur", true)
                .putBoolean("blur_pending", false)
                .putBoolean("blur_cpu_v1", true)
                .apply()
        }
    }

/** 空降助手从布尔开关升级为三档下拉：老用户的开关值只映射到「关闭 / 提示」，不擅自开启自动跳过。 */
internal fun migrateSettings2(prefs: SettingsPreferences) {
        if (!prefs.contains("sponsor_skip_mode")) {
            val legacy = if (prefs.contains("sponsor_skip")) prefs.getBoolean("sponsor_skip", true) else true
            prefs.edit().putInt("sponsor_skip_mode", if (legacy) 1 else 0).apply()
        }
    }
