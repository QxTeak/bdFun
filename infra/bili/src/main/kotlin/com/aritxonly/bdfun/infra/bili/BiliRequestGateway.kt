package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import org.json.JSONObject

/** 保留请求构造与业务错误映射；身份准备和签名通过契约回调接入。 */
class BiliRequestGateway(
    private val http: BiliHttpClient,
    private val beforeRequest: suspend () -> Unit,
    private val signQuery: suspend (MutableMap<String, String>) -> String,
    private val refreshSigningKeys: suspend () -> Unit,
    private val encode: (String) -> String,
    private val debug: Boolean,
    private val apiBase: String,
    private val passportBase: String,
    private val defaultUserAgent: String,
) {
    suspend fun dataRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = apiBase,
        method: String = "GET",
        referer: String? = null,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject {
        val root = rawRequest(
            path,
            params,
            signed,
            base,
            method,
            referer,
            userAgent = userAgent,
            headers = headers,
        )
        val code = root.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, root.optString("message")))
        return root.optJSONObject("data") ?: JSONObject()
    }

    suspend fun rawRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean = false,
        base: String = apiBase,
        method: String = "GET",
        referer: String? = null,
        tolerateEmptyBody: Boolean = false,
        userAgent: String? = null,
        headers: Map<String, String> = emptyMap(),
    ): JSONObject = withContext(Dispatchers.IO) {
        beforeRequest()
        recoverSignedRead(
            signed = signed,
            method = method,
            request = { executeRequest(path, params, signed, base, method, referer, tolerateEmptyBody, userAgent, headers) },
            denied = { it.optInt("code", 0) == -403 },
            refresh = refreshSigningKeys,
        )
    }

    private suspend fun executeRequest(
        path: String, params: Map<String, String>, signed: Boolean, base: String, method: String,
        referer: String?, tolerateEmptyBody: Boolean, userAgent: String?, headers: Map<String, String>,
    ): JSONObject {
        val request = buildBiliRequest(
            path, params, signed, base, method, referer, userAgent, headers,
        )
        if (debug) android.util.Log.d("BiliApi", "request ${request.url.encodedPath}")

        return http.execute(request, method, tolerateEmptyBody)
    }

    suspend fun postRequest(
        path: String,
        params: Map<String, String>,
        tolerateEmptyBody: Boolean = true,
    ): JSONObject {
        val result = rawRequest(
            path,
            params,
            method = "POST",
            tolerateEmptyBody = tolerateEmptyBody,
        )
        val code = result.optInt("code", 0)
        if (code != 0) throw BiliException(code, apiMessage(code, result.optString("message")))
        return result.optJSONObject("data") ?: JSONObject()
    }

    /**
     * POST 的业务字段只能放在表单里；签名参数也不能顺带把 csrf/登录凭据拼到查询串。
     * 查询串只用于无请求体的 GET，或由签名器单独返回的 GET 签名。
     */
    private suspend fun buildBiliRequest(
        path: String,
        params: Map<String, String>,
        signed: Boolean,
        base: String,
        method: String,
        referer: String?,
        userAgent: String?,
        headers: Map<String, String>,
    ): Request {
        val bodyParams = if (method == "POST") params else emptyMap()
        val queryParams = if (method == "POST") emptyMap() else params.toMutableMap()
        var url = "$base$path"
        if (signed && method != "POST") {
            url = "$url?${signQuery(queryParams.toMutableMap())}"
        } else if (queryParams.isNotEmpty()) {
            val query = queryParams.entries.joinToString("&") { (key, value) ->
                "${encode(key)}=${encode(value)}"
            }
            url = "$url?$query"
        }
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent ?: defaultUserAgent)
            .header("Referer", referer ?: if (base == passportBase) "https://passport.bilibili.com/" else "https://www.bilibili.com/")
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "zh-CN,zh;q=0.9")
        headers.forEach { (key, value) -> builder.header(key, value) }
        if (method == "POST") {
            val form = FormBody.Builder()
            bodyParams.forEach { (key, value) -> form.add(key, value) }
            builder.post(form.build())
                .header("Origin", "https://www.bilibili.com")
        }
        return builder.build()
    }
}
