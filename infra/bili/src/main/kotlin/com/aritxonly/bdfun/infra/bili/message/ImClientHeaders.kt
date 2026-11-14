package com.aritxonly.bdfun.infra.bili.message

import org.json.JSONObject
import java.util.Base64

/** 固定参考 grpc_headers.dart 的客户端环境头；不包含账号凭据。 */
internal fun imClientEnvironmentHeaders(): Map<String, String> {
    val ids = JSONObject().put("language", "zh").put("region", "CN").put("script", "Hans")
    val locale = JSONObject().put("c_locale", ids).put("s_locale", ids).put("timezone", "Asia/Shanghai")
    return mapOf(
        "bili-http-engine" to "cronet",
        "grpc-encoding" to "gzip",
        "gzip-accept-encoding" to "gzip,identity",
        "x-bili-gaia-vtoken" to "",
        "x-bili-aurora-zone" to "",
        "x-bili-exps-bin" to "",
        "x-bili-trace-id" to "11111111111111111111111111111111:1111111111111111:0:0",
        "x-bili-locale-bin" to Base64.getEncoder().encodeToString(
            imCodec.encode("bilibili.metadata.locale.Locale", locale)),
    )
}

/** 与参考 AccountManager 的 gRPC 分支一致，仅通过 metadata 鉴权，不混入 Web Cookie。 */
internal fun imTransportClient(base: okhttp3.OkHttpClient): okhttp3.OkHttpClient =
    base.newBuilder().cookieJar(okhttp3.CookieJar.NO_COOKIES).build()
