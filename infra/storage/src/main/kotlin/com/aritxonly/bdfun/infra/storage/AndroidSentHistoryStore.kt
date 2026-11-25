package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.user.SentRecord

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本机记录：用户在本 App 内发出的评论与弹幕。
 *
 * 官方接口没有「按用户拉取全部评论/弹幕」的公开列表，所以这里只记录经本 App 成功发送的内容；
 * 弹幕额外保存视频内时间点，点击记录即可带着进度跳回原视频。
 */

class SentHistoryStore(context: Context) : com.aritxonly.bdfun.core.domain.SentHistoryRepository {
    private val prefs = context.applicationContext
        .getSharedPreferences("sent_history", Context.MODE_PRIVATE)

    // 列表会随组合反复读取；整份 JSON 解析一次后缓存，写入时同步维护。
    @Volatile
    private var cache: List<SentRecord>? = null

    override fun all(): List<SentRecord> {
        cache?.let { return it }
        val raw = prefs.getString(KEY_RECORDS, null)
        val parsed = if (raw == null) emptyList() else runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val o = array.optJSONObject(i) ?: continue
                    add(
                        SentRecord(
                            id = o.optLong("id"),
                            kind = o.optString("kind", "comment"),
                            text = o.optString("text"),
                            bvid = o.optString("bvid"),
                            title = o.optString("title"),
                            videoTimeMs = o.optLong("videoTimeMs"),
                            sentAt = o.optLong("sentAt"),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyList())
        cache = parsed
        return parsed
    }

    /** 记录一条本机发出的评论 / 弹幕；重复 id 会覆盖。 */
    override fun record(
        kind: String,
        text: String,
        bvid: String,
        title: String,
        videoTimeMs: Long,
    ) {
        if (text.isBlank()) return
        val now = System.currentTimeMillis()
        add(
            SentRecord(
                id = now,
                kind = kind,
                text = text,
                bvid = bvid,
                title = title,
                videoTimeMs = videoTimeMs,
                sentAt = now,
            ),
        )
    }

    override fun add(record: SentRecord) {
        val next = (listOf(record) + all().filterNot { it.id == record.id }).take(MAX_RECORDS)
        cache = next
        val array = JSONArray()
        next.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("kind", item.kind)
                    put("text", item.text)
                    put("bvid", item.bvid)
                    put("title", item.title)
                    put("videoTimeMs", item.videoTimeMs)
                    put("sentAt", item.sentAt)
                },
            )
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    override fun clear() {
        cache = emptyList()
        prefs.edit().remove(KEY_RECORDS).apply()
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val MAX_RECORDS = 500
    }
}
