package com.aritxonly.bdfun.infra.bili

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.concurrent.ConcurrentHashMap

class BiliCookieJar(context: Context) : CookieJar {
    private val prefs: SharedPreferences = context.getSharedPreferences("bili_session", Context.MODE_PRIVATE)
    private val cookies = ConcurrentHashMap<String, Cookie>()

    init {
        prefs.getString(KEY_COOKIES, null)?.lineSequence()?.forEach { line ->
            val parts = line.split('\t')
            if (parts.size >= 5) {
                runCatching {
                    val builder = Cookie.Builder()
                        .name(parts[0])
                        .value(parts[1])
                        .domain(parts[2].removePrefix("."))
                        .path(parts[3])
                    if (parts[4].toLongOrNull()?.let { it > 0 } == true) {
                        builder.expiresAt(parts[4].toLong())
                    }
                    val cookie = builder.build()
                    cookies[cookie.name] = cookie
                }
            }
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.host.endsWith("bilibili.com")) return emptyList()
        val now = System.currentTimeMillis()
        return cookies.values.filter { cookie ->
            (cookie.expiresAt == Long.MAX_VALUE || cookie.expiresAt > now) && cookie.matches(url)
        }
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        var changed = false
        cookies.forEach {
            if (it.name.isNotBlank()) {
                this.cookies[it.name] = it
                changed = true
            }
        }
        if (changed) persist()
    }

    fun put(name: String, value: String, domain: String = "bilibili.com") {
        if (name.isBlank()) return
        cookies[name] = Cookie.Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .path("/")
            .build()
        persist()
    }

    fun value(name: String): String = cookies[name]?.value.orEmpty()

    /** 拼成 Cookie 请求头；播放器直接拉流时也要带，否则部分 CDN 会 403。 */
    fun header(): String = cookies.values.joinToString("; ") { "${it.name}=${it.value}" }

    val csrf: String get() = value("bili_jct")

    val hasSession: Boolean get() = value("SESSDATA").isNotBlank()

    val mid: Long get() = value("DedeUserID").toLongOrNull() ?: 0L

    fun clear() {
        cookies.clear()
        prefs.edit().remove(KEY_COOKIES).apply()
    }

    private fun persist() {
        val data = cookies.values.joinToString("\n") {
            listOf(
                it.name,
                it.value,
                it.domain,
                it.path,
                if (it.expiresAt == Long.MAX_VALUE) "0" else it.expiresAt.toString(),
            ).joinToString("\t")
        }
        prefs.edit().putString(KEY_COOKIES, data).apply()
    }

    private companion object {
        const val KEY_COOKIES = "cookies_v1"
    }
}
