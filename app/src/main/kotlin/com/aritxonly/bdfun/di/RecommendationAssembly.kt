package com.aritxonly.bdfun.di

import android.content.Context
import com.aritxonly.bdfun.core.application.RecommendationOptimizer
import com.aritxonly.bdfun.core.domain.RecommendationStorage
import com.aritxonly.bdfun.core.model.feed.RecommendationArchive
import com.aritxonly.bdfun.infra.storage.FileRecommendationStorage
import kotlinx.coroutines.CoroutineScope
import java.io.File
import java.security.MessageDigest

internal fun createRecommendationOptimizer(
    context: Context?, scope: CoroutineScope, owner: () -> Pair<String, Long>,
): RecommendationOptimizer {
    val storage = context?.let { FileRecommendationStorage(File(it.filesDir, "recommendation")) }
        ?: object : RecommendationStorage {
            override fun read() = RecommendationArchive()
            override fun write(archive: RecommendationArchive) = Unit
        }
    return RecommendationOptimizer(storage, scope, {
        val (account, revision) = owner()
        val key = if (account.isEmpty()) "" else MessageDigest.getInstance("SHA-256")
            .digest(account.toByteArray()).joinToString("") { "%02x".format(it) }
        key to revision
    }, log = { com.aritxonly.bdfun.data.LogCollector.record("RECOMMENDATION", it) })
}

/** 两种排序的兜底快照分开保存，关闭优化不会读到优化后缓存的顺序。 */
internal fun createHomeRecommendationCache(
    context: Context, account: Long, web: Boolean, optimizer: RecommendationOptimizer,
): com.aritxonly.bdfun.core.domain.HomeFeedCache {
    fun delegate(): com.aritxonly.bdfun.core.domain.HomeFeedCache {
        val suffix = if (optimizer.state.value.enabled) "_optimized" else ""
        return com.aritxonly.bdfun.infra.storage.FileHomeFeedCache(
            File(context.cacheDir, "home_feed/${account}_${web}$suffix.json"))
    }
    return object : com.aritxonly.bdfun.core.domain.HomeFeedCache {
        override suspend fun read() = delegate().read()
        override suspend fun write(items: List<com.aritxonly.bdfun.core.model.video.VideoItem>) = delegate().write(items)
    }
}
