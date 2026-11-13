package com.aritxonly.bdfun.infra.bili.message

import com.aritxonly.bdfun.infra.bili.BiliApiContext
import com.aritxonly.bdfun.infra.bili.BILI_APP_UA
import com.aritxonly.bdfun.infra.bili.ensureBuvid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.nio.ByteBuffer
import java.util.Base64
import java.util.zip.GZIPInputStream

internal val imCodec by lazy {
    val text = checkNotNull(ImProtoCodec::class.java.getResourceAsStream("/im-schema.json")).bufferedReader().use { it.readText() }
    ImProtoCodec(JSONObject(text))
}

internal suspend fun BiliApiContext.imRequest(method: String, requestType: String, replyType: String,
    value: JSONObject = JSONObject(), legacy: Boolean = false): JSONObject = withContext(Dispatchers.IO) {
    val access = cookieJar.value("access_key")
    check(access.isNotBlank()) { "消息功能需要 App 登录凭证，请重新扫码登录" }
    ensureBuvid()
    val buvid = cookieJar.value("buvid3")
    val metadata = JSONObject().put("access_key", access).put("mobi_app", "android_hd").put("device", "android")
        .put("build", 2001100).put("channel", "master").put("buvid", buvid).put("platform", "android")
    val device = JSONObject(metadata.toString()).put("app_id", 5).put("brand", "android").put("model", "android").put("osver", "15").put("version_name", "2.0.1")
    val prefix = if (legacy) "bilibili.im.interfaces.v1." else "bilibili.app.im.v1."
    val service = if (legacy) "bilibili.im.interface.v1.ImInterface" else "bilibili.app.im.v1.im"
    val bytes = imCodec.encode(prefix + requestType, value)
    val frame = ByteBuffer.allocate(5 + bytes.size).put(0).putInt(bytes.size).put(bytes).array()
    val request = Request.Builder().url("https://app.bilibili.com/$service/$method")
        .header("authorization", "identify_v1 $access").header("User-Agent", IM_USER_AGENT)
        .header("te", "trailers").header("grpc-accept-encoding", "gzip,identity")
        .header("x-bili-metadata-bin", metadataHeader("bilibili.metadata.Metadata", metadata))
        .header("x-bili-device-bin", metadataHeader("bilibili.metadata.device.Device", device))
        .header("x-bili-network-bin", metadataHeader("bilibili.metadata.network.Network", JSONObject().put("type", 1)))
        .header("x-bili-fawkes-req-bin", metadataHeader("bilibili.metadata.fawkes.FawkesReq",
            JSONObject().put("appkey", "android_hd").put("env", "prod").put("session_id", java.util.UUID.randomUUID().toString().take(8))))
        .header("buvid", buvid).post(frame.toRequestBody("application/grpc".toMediaType())).build()
    http.await(request).use { response ->
        check(response.isSuccessful) { "消息服务 HTTP ${response.code}" }
        val body = checkNotNull(response.body) { "消息服务返回空响应" }.bytes()
        val trailers = response.trailers()
        val status = response.header("grpc-status") ?: trailers["grpc-status"]
        check(status == "0") {
            if (debug) log("MSG", "grpc method=$method status=$status")
            imGrpcFailureText(status, response.header("grpc-message") ?: trailers["grpc-message"],
                response.header("grpc-status-details-bin") ?: trailers["grpc-status-details-bin"])
        }
        imCodec.decode(prefix + replyType, decodeImFrame(body))
    }
}

private fun metadataHeader(type: String, value: JSONObject) = Base64.getEncoder().encodeToString(imCodec.encode(type, value))

internal fun decodeImFrame(bytes: ByteArray): ByteArray {
    require(bytes.size >= 5) { "消息服务响应被截断" }
    val length = ByteBuffer.wrap(bytes, 1, 4).int
    require(length >= 0 && length == bytes.size - 5 && length <= 16 * 1024 * 1024) { "无效消息响应长度" }
    val body = bytes.copyOfRange(5, bytes.size)
    return when (bytes[0].toInt()) {
        0 -> body
        1 -> GZIPInputStream(body.inputStream()).use { input ->
            input.readNBytes(16 * 1024 * 1024 + 1).also { require(it.size <= 16 * 1024 * 1024) }
        }
        else -> error("不支持的消息压缩格式")
    }
}

private const val IM_USER_AGENT = "Mozilla/5.0 BiliDroid/2.0.1 (bbcallen@gmail.com) os/android model/android_hd mobi_app/android_hd build/2001100 channel/master innerVer/2001100 osVer/15 network/2"
