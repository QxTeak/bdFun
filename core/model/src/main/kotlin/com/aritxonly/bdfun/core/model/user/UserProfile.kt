package com.aritxonly.bdfun.core.model.user

data class UserProfile(
    val mid: Long,
    val name: String,
    val face: String,
    val sign: String,
    val level: Int,
    val coins: Double,
    val vip: Boolean,
    val vipLabel: String,
    val stats: UserProfileStats = UserProfileStats(),
    val decoration: UserProfileDecoration = UserProfileDecoration(),
    val experience: UserProfileExperience = UserProfileExperience(),
) {
    val follower get() = stats.follower
    val following get() = stats.following
    val likes get() = stats.likes
    val banner get() = decoration.banner
    val bannerFallbacks get() = decoration.bannerFallbacks
    val officialTitle get() = decoration.officialTitle
    val officialDesc get() = decoration.officialDesc
    val officialType get() = decoration.officialType
    val fansMedalName get() = decoration.fansMedalName
    val fansMedalLevel get() = decoration.fansMedalLevel
    val fansMedalColor get() = decoration.fansMedalColor
    val chargingCount get() = decoration.chargingCount
    val levelExp get() = experience.levelExp
    val nextLevelExp get() = experience.nextLevelExp
}

data class UserProfileStats(
    val follower: Long = 0,
    val following: Long = 0,
    /** 获赞数（空间页用）。 */
    val likes: Long = 0,
)

data class UserProfileDecoration(
    /** 空间头图。 */
    val banner: String = "",
    val bannerFallbacks: List<String> = emptyList(),
    /** 空间页资料：认证标题 / 等级经验（接口没有时为空 / 0）。 */
    val officialTitle: String = "",
    val officialDesc: String = "",
    val officialType: Int = 0,
    val fansMedalName: String = "",
    val fansMedalLevel: Int = 0,
    val fansMedalColor: Int = 0,
    val chargingCount: Long = 0,
)

data class UserProfileExperience(
    val levelExp: Long = 0,
    val nextLevelExp: Long = 0,
)
