package com.aritxonly.bdfun.infra.bili

import okhttp3.FormBody
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BiliRequestBuildTest {
    @Test fun postKeepsFormFieldsOutOfQueryString() = runBlocking {
        val request = buildBiliRequest(
            base = "https://api.bilibili.com",
            path = "/x/v2/dm/thumbup/add",
            params = linkedMapOf(
                "op" to "1",
                "dmid" to "10",
                "csrf" to "csrf-value",
            ),
            signed = false,
            method = "POST",
            referer = null,
            userAgent = "test",
            headers = emptyMap(),
            signQuery = { error("POST 不能进入 WBI 签名分支") },
            encode = { it },
            defaultUserAgent = "test",
            passportBase = "https://passport.bilibili.com",
        )

        assertEquals("/x/v2/dm/thumbup/add", request.url.encodedPath)
        assertNull(request.url.encodedQuery)
        assertEquals("POST", request.method)
        assertFalse(request.url.toString().contains("csrf"))
        val body = request.body as FormBody
        assertEquals("csrf-value", body.value(2))
    }
}
