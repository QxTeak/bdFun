package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.user.UserProfileStats
import com.aritxonly.bdfun.core.model.user.UserProfileExperience
import com.aritxonly.bdfun.core.model.user.UserProfileDecoration
import com.aritxonly.bdfun.core.model.user.SpaceInfo
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.domain.orderedSpaceBanners
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.biliImage as image
import org.json.JSONObject

/** 五份成功/可选响应的传输边界；不包含请求、签名或页面状态。 */
internal data class SpacePayloadsDto(
    val info: JSONObject, val stat: JSONObject?, val videos: JSONObject?,
    val nav: JSONObject?, val appSpace: JSONObject?,
)

internal fun mapSpaceInfo(
    payloads: SpacePayloadsDto, likes: Long, parseVideo: (JSONObject) -> VideoItem?,
): SpaceInfo {
    val info = payloads.info
    val videos = payloads.videos
    val nav = payloads.nav
    val vlist = videos?.optJSONObject("list")?.optJSONArray("vlist").objects().orEmpty()
    return SpaceInfo(
        profile = mapSpaceProfile(payloads, likes),
        videos = vlist.mapNotNull(parseVideo),
        videoCount = nav?.optInt("video", 0) ?: vlist.size,
        isFollowed = info.optBoolean("is_followed", false),
    )
}

private fun mapSpaceProfile(payloads: SpacePayloadsDto, likes: Long): UserProfile {
    val info = payloads.info
    val stat = payloads.stat
    val appSpace = payloads.appSpace
    val medal = info.optJSONObject("fans_medal")?.optJSONObject("medal")
    val official = info.optJSONObject("official")
    val (vipStatus, vipLabel) = readSpaceVip(appSpace)
    val charge = readSpaceCharge(appSpace)
    return UserProfile(
        mid = info.l("mid"),
        name = info.s("name"),
        face = avatar(info.s("face"), 200),
        sign = info.s("sign"),
        level = info.optInt("level", 0),
        coins = 0.0,
        vip = vipStatus == 1,
        vipLabel = vipLabel,
        stats = UserProfileStats(
            follower = stat?.l("follower") ?: 0,
            following = stat?.l("following") ?: 0,
            likes = likes,
        ),
        decoration = UserProfileDecoration(
            banner = spaceBannerCandidates(appSpace, info).firstOrNull().orEmpty(),
            bannerFallbacks = spaceBannerCandidates(appSpace, info).drop(1),
            officialTitle = official?.s("title").orEmpty(),
            officialDesc = official?.s("desc").orEmpty(),
            officialType = official?.optInt("type", 0) ?: 0,
            fansMedalName = medal?.s("medal_name").orEmpty(),
            fansMedalLevel = medal?.optInt("level", 0) ?: 0,
            fansMedalColor = medal?.optInt("medal_color", 0) ?: 0,
            chargingCount = charge,
        ),
        experience = UserProfileExperience(
            levelExp = info.optLong("level_exp", 0L),
            nextLevelExp = info.optLong("next_level_exp", 0L),
        ),
    )
}

private data class SpaceVipFields(val vipStatus: Int, val vipLabel: String)
private fun readSpaceVip(appSpace: JSONObject?): SpaceVipFields {
    val cardVip = appSpace?.optJSONObject("card")?.optJSONObject("vip")
    val vipStatus = cardVip?.optInt("status", 0)
        ?.takeIf { it > 0 }
        ?: (cardVip?.optInt("vipStatus", 0) ?: 0)
    val vipLabelObj = cardVip?.optJSONObject("label")
    val vipTheme = vipLabelObj?.s("label_theme").orEmpty()
    val vipType = cardVip?.optInt("type", 0)
        ?.takeIf { it > 0 }
        ?: (cardVip?.optInt("vipType", 0) ?: 0)
    val vipLabel = vipLabelObj?.s("text").orEmpty().ifBlank {
        when {
            vipTheme.contains("hundred", ignoreCase = true) -> "百年大会员"
            vipTheme.contains("ten_year", ignoreCase = true) -> "十年大会员"
            vipTheme.contains("ten", ignoreCase = true) -> "十年大会员"
            vipTheme.contains("annual", ignoreCase = true) -> "年度大会员"
            vipType == 2 -> "年度大会员"
            vipStatus == 1 -> "大会员"
            else -> ""
        }
    }
    return SpaceVipFields(vipStatus, vipLabel)
}

internal fun readSpaceCharge(appSpace: JSONObject?): Long {
    val elec = appSpace?.optJSONObject("elec")
    val charge = elec?.l("total")?.takeIf { it > 0L }
        ?: elec?.optJSONArray("list")?.length()?.toLong()?.takeIf { it > 0L }
        ?: 0L
    return charge
}

internal fun spaceBannerCandidates(payload: JSONObject?, info: JSONObject): List<String> {
    val images = payload?.optJSONObject("images")
    val web = info.s("top_photo").ifBlank { info.optJSONObject("card")?.s("top_photo").orEmpty() }
        .ifBlank { info.optJSONObject("space")?.s("top_photo").orEmpty() }
    // 只认 App 空间装扮图；网页版 top_photo 与收藏集 cover 的比例不适合通栏头图，仅作兜底。
    return orderedSpaceBanners(web, images?.s("imgUrl").orEmpty(), images?.s("night_imgurl").orEmpty())
        .map(::image)
}
