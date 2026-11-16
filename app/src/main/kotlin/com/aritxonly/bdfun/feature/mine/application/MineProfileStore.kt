package com.aritxonly.bdfun.feature.mine.application

import com.aritxonly.bdfun.core.model.user.UserProfile

/** 保留原先单账号、五分钟缓存；由进程容器持有，退出账号时清理。 */
internal class MineProfileStore(private val now: () -> Long) {
    private var profile: UserProfile? = null
    private var loadedAt = 0L
    private var owner = 0L
    fun snapshot(mid: Long): UserProfile? = profile?.takeIf { owner == mid }
    fun cached(mid: Long): UserProfile? = profile?.takeIf { owner == mid && now() - loadedAt < TTL }
    fun put(mid: Long, value: UserProfile, startedAt: Long) { owner = mid; profile = value; loadedAt = startedAt }
    fun timestamp() = now()
    fun clear() { profile = null; loadedAt = 0 }
    private companion object { const val TTL = 5 * 60 * 1000L }
}
