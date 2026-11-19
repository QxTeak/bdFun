package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.PopularPage

/**
 * 首页「热门」段的读取接口。
 *
 * 接口是 PiliPlus 同款 `/x/web-interface/popular`（pn/ps 分页），
 * 实现放在 infra:bili，页面只依赖这一层。
 */
interface PopularRepository {
    suspend fun page(page: Int = 1): Outcome<PopularPage>
}
