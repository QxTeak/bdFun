package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest

/** 保留六小时刷新与旧密钥兜底；请求和 JSON 解析由外部提供。 */
class WbiSigner(
    private val loadKeys: suspend () -> Pair<String, String>?,
    private val encode: (String) -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private data class Snapshot(val keys: WbiKeys, val fetchedAt: Long)
    private val mutex = Mutex()
    @Volatile private var snapshot: Snapshot? = null

    val hasKeys: Boolean get() = snapshot != null

    fun update(imgKey: String, subKey: String) {
        if (imgKey.isNotBlank() && subKey.isNotBlank()) {
            snapshot = Snapshot(WbiKeys(imgKey, subKey), clock())
        }
    }

    fun clear() { snapshot = null }

    suspend fun keys(): WbiKeys {
        snapshot?.takeIf { clock() - it.fetchedAt < TTL_MS }?.let { return it.keys }
        return mutex.withLock {
            val current = snapshot
            if (current != null && clock() - current.fetchedAt < TTL_MS) return@withLock current.keys
            try {
                loadKeys()?.let { (img, sub) -> update(img, sub) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (current != null) return@withLock current.keys
                throw BiliException(-1, "无法获取请求签名，请检查网络")
            }
            snapshot?.keys ?: current?.keys ?: throw BiliException(-1, "无法获取请求签名，请检查网络")
        }
    }

    suspend fun forceRefresh() {
        val previous = snapshot
        snapshot = null
        try {
            keys()
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            if (snapshot == null) snapshot = previous
            throw failure
        }
    }

    suspend fun signQuery(params: MutableMap<String, String>): String {
        val keys = keys()
        params["wts"] = (clock() / 1000).toString()
        val query = params.toSortedMap().entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value.replace(wbiUnsafeChars, ""))}"
        }
        return "$query&w_rid=${md5(query + keys.mixinKey)}"
    }

    companion object {
        private const val TTL_MS = 6 * 60 * 60 * 1000L
        internal fun md5(value: String): String {
            val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
            return hex(digest)
        }
    }
}

private val wbiUnsafeChars = Regex("[!'()*]")
private val HEX_DIGITS = "0123456789abcdef".toCharArray()

internal fun hex(bytes: ByteArray): String {
    val out = CharArray(bytes.size * 2)
    for (i in bytes.indices) {
        val v = bytes[i].toInt() and 0xFF
        out[i * 2] = HEX_DIGITS[v ushr 4]
        out[i * 2 + 1] = HEX_DIGITS[v and 0x0F]
    }
    return String(out)
}

class WbiKeys(val imgKey: String, val subKey: String) {
    val mixinKey: String = run {
        val raw = imgKey + subKey
        MIXIN_TABLE.map { raw.getOrElse(it) { '0' } }.joinToString("").take(32)
    }

    private companion object {
        val MIXIN_TABLE = intArrayOf(
            46, 47, 18, 2, 53, 8, 23, 32, 15, 50, 10, 31, 58, 3, 45, 35,
            27, 43, 5, 49, 33, 9, 42, 19, 29, 28, 14, 39, 12, 38, 41, 13,
            37, 48, 7, 16, 24, 55, 40, 61, 26, 17, 0, 1, 60, 51, 30, 4,
            22, 25, 54, 21, 56, 59, 6, 63, 57, 62, 11, 36, 20, 34, 44, 52,
        )
    }
}
