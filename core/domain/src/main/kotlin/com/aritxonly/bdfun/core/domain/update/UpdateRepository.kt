package com.aritxonly.bdfun.core.domain.update

import com.aritxonly.bdfun.core.common.Outcome

interface UpdateRepository {
    suspend fun releases(): Outcome<List<UpdateRelease>>
    suspend fun latest(includePreview: Boolean): Outcome<UpdateRelease?> = when (val result = releases()) {
        is Outcome.Success -> Outcome.Success(latestRelease(result.value, includePreview))
        is Outcome.Failure -> result
    }
}
