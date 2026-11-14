package com.aritxonly.bdfun.infra.bili.message

import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Base64

/** 与固定上游 msg.dart 的读取参数一致，GET 设置接口也需要完整客户端与 CSRF 参数。 */
internal fun imChatReadParams(csrf: String, params: Map<String, String>): Map<String, String> =
    params + mapOf("build" to "0", "mobi_app" to "web", "csrf" to csrf, "csrf_token" to csrf)

internal suspend fun <T> imChatReadStage(stage: String, block: suspend () -> T): T = try {
    block()
} catch (cancel: CancellationException) {
    throw cancel
} catch (failure: Exception) {
    throw IllegalStateException("${stage}加载失败：${failure.message ?: "请求失败"}", failure)
}

// Status 取自固定上游 bilibili/rpc.pbjson.dart；Any 为 protobuf 标准 type_url/value 结构。
private val errorCodec = ImProtoCodec(JSONObject("""{
  "bilibili.rpc.Status":[
    {"1":"code","3":1,"4":1,"5":5},
    {"1":"message","3":2,"4":1,"5":9},
    {"1":"details","3":3,"4":3,"5":11,"6":".google.protobuf.Any"}],
  "google.protobuf.Any":[
    {"1":"type_url","3":1,"4":1,"5":9},
    {"1":"value","3":2,"4":1,"5":12}]
}"""))

internal fun imGrpcFailureText(status: String?, message: String?, details: String?): String {
    val decoded = details?.takeIf { it.length <= 64 * 1024 }?.let { encoded ->
        try { errorCodec.decode("bilibili.rpc.Status", Base64.getDecoder().decode(encoded)) }
        catch (_: Exception) { null }
    }
    val reasons = decoded?.optJSONArray("details").imObjects().mapNotNull { detail ->
        try {
            val error = errorCodec.decode("bilibili.rpc.Status", Base64.getDecoder().decode(detail.optString("value")))
            error.optString("message").takeIf { it.isNotBlank() }?.let { text ->
                val code = error.optInt("code")
                imBusinessErrorText(code, text)
            }
        } catch (_: Exception) { null }
    }
    if (reasons.isNotEmpty()) return reasons.joinToString("；").take(1024)
    if (decoded?.optInt("code") == -663) {
        return imBusinessErrorText(-663, decoded.optString("message").ifBlank { "鉴权失败" })
    }
    val text = decoded?.optString("message")?.takeIf { it.isNotBlank() } ?: message?.let {
        try { URLDecoder.decode(it.replace("+", "%2B"), "UTF-8") }
        catch (_: IllegalArgumentException) { it }
    }
    return if (text.isNullOrBlank()) "消息服务失败 (${status ?: "未知状态"})" else text.take(1024)
}

private fun imBusinessErrorText(code: Int, text: String): String = when (code) {
    -663 -> "$text (-663)；请退出账号后重新扫码登录，以更新消息登录凭证"
    0 -> text
    else -> "$text ($code)"
}
