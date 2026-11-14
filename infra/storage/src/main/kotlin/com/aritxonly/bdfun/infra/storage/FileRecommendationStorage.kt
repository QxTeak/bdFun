package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.RecommendationStorage
import com.aritxonly.bdfun.core.model.feed.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject

/** 独立、可忽略的 v1 文件，不修改既有 settings/history；临时文件替换避免半写入。 */
class FileRecommendationStorage(private val directory: File) : RecommendationStorage {
    private val file get() = File(directory, "recommendation_v1.json")
    override fun read(): RecommendationArchive {
        if (!file.exists()) return RecommendationArchive()
        check(file.length() <= MAX_BYTES) { "推荐记录超过预算" }
        val root = JSONObject(file.readText())
        if (root.optInt("version") != 1) return RecommendationArchive()
        val rows = root.optJSONArray("records") ?: JSONArray()
        val accounts = linkedMapOf<String, MutableList<RecommendationMemory>>()
        for (index in 0 until minOf(rows.length(), 5000)) {
            val row = rows.optJSONObject(index) ?: continue
            val key = row.optString("account")
            val bvid = row.optString("bv")
            if (key.isBlank() || bvid.isBlank()) continue
            accounts.getOrPut(key) { mutableListOf() } += RecommendationMemory(
                bvid, row.optLong("up"), row.optString("category"), row.optLong("updated"),
                row.optLong("exposed"), row.optLong("clicked"), row.optDouble("satisfaction", 0.0).let {
                    if (it.isFinite()) it.coerceIn(0.0, 1.0) else 0.0
                }, row.optLong("watched"))
        }
        return RecommendationArchive(root.optBoolean("enabled", false), accounts)
    }

    override fun write(archive: RecommendationArchive) {
        val rows = JSONArray()
        archive.accounts.forEach { (key, values) -> values.forEach { value ->
            rows.put(JSONObject().put("account", key).put("bv", value.bvid).put("up", value.owner)
                .put("category", value.category.take(128)).put("updated", value.updatedAt)
                .put("exposed", value.exposedAt).put("clicked", value.clickedAt).put("satisfaction", value.satisfaction).put("watched", value.watchedAt))
        } }
        val text = JSONObject().put("version", 1).put("enabled", archive.enabled).put("records", rows).toString()
        check(text.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "推荐记录超过预算" }
        check(directory.exists() || directory.mkdirs())
        val temporary = File(directory, "recommendation_v1.tmp")
        temporary.outputStream().use { stream -> stream.write(text.toByteArray(Charsets.UTF_8)); stream.fd.sync() }
        check(temporary.renameTo(file)) { "推荐记录替换失败" }
    }
    companion object { const val MAX_BYTES = 4 * 1024 * 1024 }
}
