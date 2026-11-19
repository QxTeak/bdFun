package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.PopularRepository
import com.aritxonly.bdfun.core.model.video.PopularPage

/** 首页「热门」段：PiliPlus 同款 `/x/web-interface/popular`，按 pn 分页。 */
class BiliPopularRepository(private val api: BiliBackend) : PopularRepository {
    override suspend fun page(page: Int): Outcome<PopularPage> = biliOutcome { api.popular(page) }
}
