package com.aritxonly.bdfun.infra.bili

import android.content.Context
import android.net.Uri
import android.text.Html
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Dispatcher
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import kotlin.math.roundToInt
import com.aritxonly.bdfun.core.domain.*
import com.aritxonly.bdfun.core.model.video.*
import com.aritxonly.bdfun.core.model.comment.*
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.feed.*
import com.aritxonly.bdfun.core.model.message.*
import com.aritxonly.bdfun.core.model.library.*
import com.aritxonly.bdfun.infra.bili.mapper.*
import com.aritxonly.bdfun.infra.bili.BILI_API as API
import com.aritxonly.bdfun.infra.bili.BILI_APP_API as APP_API
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA as APP_UA
import com.aritxonly.bdfun.infra.bili.BILI_APP as APP
import com.aritxonly.bdfun.infra.bili.BILI_APP_KEY as APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_APP_SEC as APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_KEY as TV_APP_KEY
import com.aritxonly.bdfun.infra.bili.BILI_TV_APP_SEC as TV_APP_SEC
import com.aritxonly.bdfun.infra.bili.BILI_APP_HEADERS as APP_HEADERS
import com.aritxonly.bdfun.infra.bili.BILI_PASSPORT as PASSPORT
import com.aritxonly.bdfun.infra.bili.BILI_VC as VC
import com.aritxonly.bdfun.infra.bili.BILI_UA as UA
import com.aritxonly.bdfun.infra.bili.encodeBiliParameter as encode
import com.aritxonly.bdfun.infra.bili.biliMd5 as md5
import com.aritxonly.bdfun.infra.bili.biliImage as image
import com.aritxonly.bdfun.infra.bili.biliAvatar as avatar
import com.aritxonly.bdfun.infra.bili.cleanBiliHtml as cleanHtml
import com.aritxonly.bdfun.infra.bili.biliAidToBvid as aidToBvid
import com.aritxonly.bdfun.infra.bili.parseBiliCountText as parseCountText
import com.aritxonly.bdfun.infra.bili.parseBiliDurationText as parseDurationText
import com.aritxonly.bdfun.infra.bili.biliQualityLabel as qualityLabel

internal suspend fun BiliApiContext.nav(): UserProfile? = withContext(Dispatchers.IO) {
    val root = rawRequest("/x/web-interface/nav", emptyMap())
    parseWbi(root.optJSONObject("data")?.optJSONObject("wbi_img"))
    val data = root.optJSONObject("data") ?: return@withContext null
    mapAccountProfile(data)
}

/**
 * HD 端扫码登录：比网页扫码多拿一个 access_key，并与 IM 的客户端身份保持一致。
 * access_key 存在 cookieJar 里，`signAppParams` 会自动带上，
 * `/x/v2/space` 这类 App 接口才能返回 IP 属地等字段。
 */

internal suspend fun BiliApiContext.qrGenerate(): QrLogin = withContext(Dispatchers.IO) {
    val params = HdClientIdentity.qrParams(appSigner)
    val data = dataRequest(
        path = "/x/passport-tv-login/qrcode/auth_code",
        params = params,
        base = PASSPORT,
        method = "POST",
        userAgent = HdClientIdentity.userAgent,
        headers = mapOf("app-key" to HdClientIdentity.mobiApp),
    )
    mapQrLogin(data)
}

internal suspend fun BiliApiContext.qrPoll(key: String): QrPoll = withContext(Dispatchers.IO) {
    val params = HdClientIdentity.qrParams(appSigner, key)
    val root = rawRequest(
        path = "/x/passport-tv-login/qrcode/poll",
        params = params,
        base = PASSPORT,
        method = "POST",
        userAgent = HdClientIdentity.userAgent,
        headers = mapOf("app-key" to HdClientIdentity.mobiApp),
    )
    when (root.optInt("code", -1)) {
        0 -> {
            val data = root.optJSONObject("data") ?: return@withContext QrPoll(
                QrState.Error,
                "登录失败：没有返回登录信息",
            )
            saveTvLogin(data)
            val profile = nav()
            QrPoll(QrState.Success, "登录成功", profile)
        }
        else -> mapQrPollFailure(root)
    }
}

internal fun BiliApiContext.saveTvLogin(data: JSONObject) {
    val cookies = data.optJSONObject("cookie_info")
        ?.optJSONArray("cookies")
        .objects()
    cookies.forEach { cookie ->
        val name = cookie.s("name")
        val value = cookie.s("value")
        if (name.isNotBlank() && value.isNotBlank()) cookieJar.put(name, value)
    }
    // access_key 也存进同一个 cookie 容器，App 签名和退出登录都走它。
    val accessKey = data.s("access_token")
        .ifBlank { data.optJSONObject("token_info")?.s("access_token").orEmpty() }
    if (accessKey.isNotBlank()) cookieJar.put("access_key", accessKey)
}

internal fun BiliApiContext.logout() {
    cookieJar.clear()
    wbiSigner.clear()
    buvidRetryAt = 0L
}

internal fun BiliApiContext.saveLoginCookies(url: String) {
    val uri = Uri.parse(url)
    listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5").forEach { name ->
        uri.getQueryParameter(name)?.takeIf { it.isNotBlank() }?.let { cookieJar.put(name, it) }
    }
}

// ---------------------------------------------------------------- feed

internal suspend fun BiliApiContext.ensureBuvid() {
    if (cookieJar.value("buvid3").isNotBlank()) return
    if (System.currentTimeMillis() < buvidRetryAt) return
    buvidMutex.withLock {
        if (cookieJar.value("buvid3").isNotBlank()) return
        if (System.currentTimeMillis() < buvidRetryAt) return
        val ok = try {
            val request = Request.Builder()
                .url("$API/x/frontend/finger/spi")
                .header("User-Agent", UA)
                .header("Referer", "https://www.bilibili.com/")
                .get()
                .build()
            http.await(request).use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body?.string().orEmpty()
                val data = JSONObject(body).optJSONObject("data")
                    ?: throw IOException("buvid data is empty")
                val b3 = data.s("b_3")
                if (b3.isBlank()) throw IOException("buvid3 is empty")
                val b4 = data.s("b_4")
                cookieJar.put("buvid3", b3)
                if (b4.isNotBlank()) cookieJar.put("buvid4", b4)
                cookieJar.put("b_nut", (System.currentTimeMillis() / 1000).toString())
                true
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (debug) android.util.Log.w("BiliApi", "buvid fetch failed", e)
            false
        }
        if (ok) {
            buvidRetryAt = 0L
        } else {
            // 失败后 30 秒内不再重试，否则每个接口都会先卡一次 buvid 请求。
            buvidRetryAt = System.currentTimeMillis() + 30_000L
        }
    }
}

internal fun BiliApiContext.parseWbi(wbi: JSONObject?) {
    if (wbi == null) return
    val img = wbi.s("img_url").substringAfterLast('/').substringBefore('.')
    val sub = wbi.s("sub_url").substringAfterLast('/').substringBefore('.')
    if (img.isNotBlank() && sub.isNotBlank()) {
        wbiSigner.update(img, sub)
    }
}
