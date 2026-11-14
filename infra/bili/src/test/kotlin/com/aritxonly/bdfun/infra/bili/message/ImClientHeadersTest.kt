package com.aritxonly.bdfun.infra.bili.message

import org.junit.Assert.*
import org.junit.Test
import java.util.Base64

class ImClientHeadersTest {
    @Test fun grpcTransportDoesNotSendWebCookiesOrPersistResponseCookies() {
        var loads = 0
        var saves = 0
        val jar = object : okhttp3.CookieJar {
            override fun loadForRequest(url: okhttp3.HttpUrl): List<okhttp3.Cookie> {
                loads++
                return listOf(okhttp3.Cookie.Builder().name("SESSDATA").value("test").domain("bilibili.com").build())
            }
            override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<okhttp3.Cookie>) { saves++ }
        }
        val base = okhttp3.OkHttpClient.Builder().cookieJar(jar).build()
        val grpc = imTransportClient(base)
        val url = okhttp3.HttpUrl.Builder().scheme("https").host("app.bilibili.com").build()
        assertTrue(grpc.cookieJar.loadForRequest(url).isEmpty())
        grpc.cookieJar.saveFromResponse(url, emptyList())
        assertEquals(0, loads)
        assertEquals(0, saves)
        assertSame(jar, base.cookieJar)
        assertSame(base.connectionPool, grpc.connectionPool)
        assertSame(base.dispatcher, grpc.dispatcher)
        assertEquals("gzip", imClientEnvironmentHeaders()["grpc-encoding"])
    }

    @Test fun environmentIncludesEngineAndPinnedLocaleWithoutCredentials() {
        val headers = imClientEnvironmentHeaders()
        assertEquals("cronet", headers["bili-http-engine"])
        assertFalse(headers.containsKey("authorization"))
        val locale = imCodec.decode("bilibili.metadata.locale.Locale",
            Base64.getDecoder().decode(headers.getValue("x-bili-locale-bin")))
        assertEquals("Asia/Shanghai", locale.getString("timezone"))
        for (key in listOf("c_locale", "s_locale")) {
            assertEquals("zh", locale.getJSONObject(key).getString("language"))
            assertEquals("Hans", locale.getJSONObject(key).getString("script"))
            assertEquals("CN", locale.getJSONObject(key).getString("region"))
        }
    }

    @Test fun privateSessionUpdateMatchesIndependentWireSample() {
        // SessionUpdateReq.session_id.private_id.talker_uid = 123，逐层长度编码。
        val request = org.json.JSONObject().put("session_id", privateIdentity(123))
        assertArrayEquals(byteArrayOf(10, 4, 10, 2, 8, 123),
            imCodec.encode("bilibili.app.im.v1.SessionUpdateReq", request))
    }
}
