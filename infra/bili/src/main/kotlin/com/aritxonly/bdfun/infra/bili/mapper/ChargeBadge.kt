package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONObject

/**
 * 判断接口返回的稿件是否包月充电专属。
 *
 * B 站不同列表接口给的字段不一样，历史上见过这几种：
 * - `charging_pay.level`（App 推荐 / 稍后再看 / 热门）
 * - `is_charging_arc`（空间投稿、部分搜索接口）
 * - `is_charging`（新版稿件字段）
 * - `badge.text == "充电专属"`（动态卡）
 * - `rights.is_charge` / `rights.is_charging`
 *
 * 任何一个命中就视为充电视频；字段缺失时一律返回 false，不猜。
 */
internal fun JSONObject.hasChargeBadge(): Boolean {
    val chargingPay = optJSONObject("charging_pay")
    if (chargingPay != null && !chargingPay.isNull("level")) return true
    if (optBoolean("is_charging")) return true
    if (optBoolean("is_charging_arc")) return true
    if (optJSONObject("badge")?.optString("text").orEmpty().contains("充电")) return true
    val rights = optJSONObject("rights")
    if (rights != null && (rights.optInt("is_charge", 0) == 1 || rights.optInt("is_charging", 0) == 1)) return true
    return false
}
