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
