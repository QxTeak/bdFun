package com.aritxonly.bdfun.infra.bili

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.ResponseBody.Companion.toResponseBody

class BiliRequestBuildTest {
    @Test fun postRequestKeepsFormFieldsOutOfQueryString() = runBlocking {
        var captured: okhttp3.Request? = null
        val http = BiliHttpClient(
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    captured = chain.request()
                    okhttp3.Response.Builder()
                        .request(chain.request())
                        .protocol(okhttp3.Protocol.HTTP_1_1)
                        .code(200)
                        .message("OK")
                        .body("""{"code":0,"data":{}}""".toResponseBody())
                        .build()
                }
                .build(),
            debug = false,
            log = { _, _ -> },
        )
        val gateway = BiliRequestGateway(
            http = http,
            beforeRequest = {},
            signQuery = { error("POST 不能进入 WBI 签名分支") },
            refreshSigningKeys = {},
            encode = { it },
            debug = false,
            apiBase = "https://api.bilibili.com",
            passportBase = "https://passport.bilibili.com",
            defaultUserAgent = "test",
        )

        gateway.postRequest(
            path = "/x/v2/dm/thumbup/add",
            params = linkedMapOf(
                "op" to "1",
                "dmid" to "10",
                "csrf" to "csrf-value",
            ),
        )

        val request = requireNotNull(captured)
        assertEquals("/x/v2/dm/thumbup/add", request.url.encodedPath)
        assertNull(request.url.encodedQuery)
        assertEquals("POST", request.method)
        assertFalse(request.url.toString().contains("csrf"))
        val body = request.body as FormBody
        assertEquals("csrf-value", body.value(2))
    }
}
