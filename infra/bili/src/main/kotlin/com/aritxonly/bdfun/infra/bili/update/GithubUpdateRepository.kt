package com.aritxonly.bdfun.infra.bili.update

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.update.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

class GithubUpdateRepository : UpdateRepository {
    private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
    override suspend fun releases(): Outcome<List<UpdateRelease>> = withContext(Dispatchers.IO) {
        try {
            val releases = mutableListOf<UpdateRelease>()
            // releases/latest 排除 preview，因此必须读取列表。
            for (page in 1..5) {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/QxTeak/bdFun/releases?per_page=100&page=$page")
                    .header("Accept", "application/vnd.github+json").header("User-Agent", "bdFun")
                    .header("X-GitHub-Api-Version", "2022-11-28").build()
                val array = client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "GitHub HTTP ${response.code}" }
                    JSONArray(requireNotNull(response.body).string())
                }
                for (i in 0 until array.length()) {
                    githubRelease(array.getJSONObject(i))?.let(releases::add)
                }
                if (array.length() < 100) break
            }
            Outcome.Success(releases.sortedByDescending { it.version.code })
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { Outcome.Failure(AppError.Network(e.message ?: "更新检查失败")) }
    }
}
