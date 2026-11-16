package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.common.AppError
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.HomeFeedCache
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.core.navigation.Route
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/** 每个账号和推荐来源独立保存，最多保留一屏附近的数据。 */
class FileHomeFeedCache(private val file: File) : HomeFeedCache {
    private val codec = RouteJsonCodec()
    override suspend fun read(): Outcome<List<VideoItem>> = access {
        if (!file.exists()) emptyList() else {
            val values = JSONArray(file.readText())
            (0 until values.length()).mapNotNull { (codec.decode(values.getString(it)) as? Route.Video)?.snapshot }
        }
    }
    override suspend fun write(items: List<VideoItem>): Outcome<Unit> = access {
        file.parentFile?.mkdirs()
        val values = JSONArray()
        items.take(24).forEach { values.put(codec.encode(Route.Video(it.bvid, it.aid, it))) }
        val temp = File(file.path + ".tmp")
        temp.writeText(values.toString())
        if (!temp.renameTo(file)) { temp.delete(); throw java.io.IOException("无法替换首页缓存") }
    }
    private suspend fun <T> access(block: () -> T): Outcome<T> = withContext(Dispatchers.IO) {
        try { Outcome.Success(block()) }
        catch (cancel: CancellationException) { throw cancel }
        catch (failure: Exception) { Outcome.Failure(AppError.Unknown(failure.message ?: "首页缓存不可用")) }
    }
}
