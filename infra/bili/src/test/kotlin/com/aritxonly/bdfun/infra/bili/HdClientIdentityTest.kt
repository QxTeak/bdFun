package com.aritxonly.bdfun.infra.bili

import org.junit.Assert.*
import org.junit.Test

class HdClientIdentityTest {
    private val signer = AppRequestSigner({ it }, { 1700000000000L })

    @Test fun qrGenerationUsesHdIdentityAndNeverTheTvSigningKey() {
        val params = HdClientIdentity.qrParams(signer)
        assertEquals("android_hd", params["mobi_app"])
        assertEquals("android", params["platform"])
        assertEquals("0", params["local_id"])
        assertEquals(BILI_APP_KEY, params["appkey"])
        assertNotEquals(BILI_TV_APP_KEY, params["appkey"])
        assertFalse(params.containsKey("access_key"))
        assertFalse(params.containsKey("auth_code"))
        assertTrue(HdClientIdentity.userAgent.contains("mobi_app/${params["mobi_app"]}"))
        verifySignature(params)
    }

    @Test fun pollingKeepsQrKeyAndSameSigningIdentityWithoutAccountCredentials() {
        val params = HdClientIdentity.qrParams(signer, "test-code")
        assertEquals("test-code", params["auth_code"])
        assertEquals(BILI_APP_KEY, params["appkey"])
        assertEquals(setOf("auth_code", "local_id", "appkey", "ts", "sign"), params.keys)
        verifySignature(params)
    }

    private fun verifySignature(params: Map<String, String>) {
        val query = params.filterKeys { it != "sign" }.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        assertEquals(WbiSigner.md5(query + BILI_APP_SEC), params["sign"])
    }
}
