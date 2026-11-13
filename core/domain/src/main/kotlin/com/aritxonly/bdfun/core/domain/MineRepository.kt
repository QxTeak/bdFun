package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.user.UserProfile

interface MineRepository {
    suspend fun profile(): Outcome<UserProfile?>
    suspend fun relationStat(mid: Long): Outcome<Pair<Long, Long>>
    suspend fun likes(mid: Long): Outcome<Long>
    suspend fun decoration(mid: Long): Outcome<com.aritxonly.bdfun.core.model.user.UserProfileDecoration?> =
        Outcome.Success(null)
}
