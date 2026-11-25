package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.infra.bili.isRetryableHttpStatus
import com.aritxonly.bdfun.infra.bili.httpRetryDelayMs
import com.aritxonly.bdfun.infra.bili.httpRequestAttempts
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private val playUrlPattern = Regex("https?://[^\"\\s]+")
private val playUrlSecretPattern = Regex("\"(e|upsig|sign|deadline|trid|w_rid|uipk|qn_dyeid)\":\"[^\"]*\"")

/** 只负责 HTTP 执行与原有重试；Cookie、连接池由启动层传入。 */
class BiliHttpClient(
    private val client: OkHttpClient,
    private val debug: Boolean,
    private val log: (String, String) -> Unit,
) {
    suspend fun await(request: Request): Response = client.newCall(request).awaitResponse()

    suspend fun execute(
        request: Request,
        method: String,
        tolerateEmptyBody: Boolean,
    ): JSONObject = withContext(Dispatchers.IO) {
        // 只对 GET 做自动重试：POST 可能是投币/点赞，重复发送会有副作用。
        // 最多两次：一次正常、一次重试；避免网络完全断开时转圈太久。
        val attempts = httpRequestAttempts(method)
        var lastError: Exception? = null
        for (attempt in 0 until attempts) {
            try {
                return@withContext executeOnce(request, tolerateEmptyBody)
            } catch (e: CancellationException) {
                throw e
            } catch (e: BiliException) {
                // 200 空 body / 解析失败也当作可重试的瞬时问题（仅 GET）。
                if (method == "GET" && e.code == -1 && attempt < attempts - 1) {
                    lastError = e
                } else {
                    throw e
                }
            } catch (e: RetryableHttpException) {
                lastError = BiliException(e.statusCode, "网络错误 HTTP ${e.statusCode}")
            } catch (e: IOException) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            } catch (e: Exception) {
                lastError = BiliException(-1, "网络连接失败，请检查网络后重试")
            }
            if (attempt < attempts - 1) delay(httpRetryDelayMs(attempt))
        }
        val error = lastError ?: BiliException(-1, "网络连接失败，请检查网络后重试")
        log(
            "API",
            "fail path=${request.url.encodedPath} status=${(error as? BiliException)?.code} " +
                "message=${error.message}",
        )
        if (debug) android.util.Log.w("BiliApi", "network error ${request.url.encodedPath}", error)
        throw error
    }

    private class RetryableHttpException(val statusCode: Int) : IOException("HTTP $statusCode")

    /** OkHttp 的异步回调转成可取消的 suspend，避免 execute() 占死 IO 线程且无法随页面取消。 */
    private suspend fun Call.awaitResponse(): Response = suspendCancellableCoroutine { continuation ->
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (!continuation.isCancelled) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        continuation.resume(response)
                    } else {
                        response.close()
                    }
                }
            },
        )
        continuation.invokeOnCancellation { runCatching { cancel() } }
    }

    suspend fun executeOnce(
        request: Request,
        tolerateEmptyBody: Boolean,
    ): JSONObject = client.newCall(request).awaitResponse().use { response ->
        if (isRetryableHttpStatus(response.code)) {
            throw RetryableHttpException(response.code)
        }
        if (response.code >= 400) {
            throw BiliException(response.code, "网络错误 HTTP ${response.code}")
        }
        val body = response.body?.string().orEmpty()
        if (debug && request.url.encodedPath.contains("playurl")) {
            val safeBody = body.replace(playUrlPattern, "[url]")
                .replace(playUrlSecretPattern, "\"\$1\":\"[redacted]\"")
                .take(1600)
            log(
                "API",
                "http playurl code=${response.code} type=${response.header("Content-Type")} " +
                    "len=${response.header("Content-Length")} bodyLen=${body.length} " +
                    "encoding=${response.header("Content-Encoding")} " +
                    "server=${response.header("Server")} via=${response.header("Via")} " +
                    "body=$safeBody",
            )
        }
        if (body.isBlank()) {
            // 点赞/投币/收藏等动作接口成功时偶尔返回 2xx 空 body，不能当成“解析失败”
            if (tolerateEmptyBody && response.code in 200..299) return@use JSONObject()
            throw BiliException(-1, "服务器返回为空")
        }
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            throw BiliException(-1, "数据解析失败")
        }
        if (debug) {
            android.util.Log.d(
                "BiliApi",
                "response ${request.url} http=${response.code} code=${json.optInt("code", 0)}",
            )
        }
        json
    }

}
