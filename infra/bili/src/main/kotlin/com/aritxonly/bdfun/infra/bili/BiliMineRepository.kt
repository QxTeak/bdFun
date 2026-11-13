package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.domain.MineRepository

class BiliMineRepository(private val backend: BiliBackend) : MineRepository {
    override suspend fun profile() = biliOutcome { backend.nav() }
    override suspend fun relationStat(mid: Long) = biliOutcome { backend.relationStat(mid) }
    override suspend fun decoration(mid: Long) = biliOutcome { backend.space(mid).profile.decoration }
    override suspend fun likes(mid: Long) = biliOutcome { backend.userLikes(mid) }
}
