package com.aritxonly.bdfun.infra.bili

import java.net.HttpURLConnection
import java.net.URL
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test

class VideoLinkResolverTest {
    private val bv = "BV1orW46aEba"
    private class Connection(url: URL, private val location: String?) : HttpURLConnection(url) {
        var disconnected = false
        var connectedCount = 0
        override fun connect() { connectedCount++ }
        override fun disconnect() { disconnected = true }
        override fun usingProxy() = false
        override fun getHeaderField(name: String?): String? = if (name == "Location") location else null
    }
    @Test fun directBvidDoesNotOpenAnyConnection() {
        assertEquals(bv, resolveVideoLink("prefix bv" + bv.substring(2)) { error("network must not run") })
        assertNull(resolveVideoLink("av123") { error("network must not run") })
    }
    @Test fun fullAidUrlResolvesLocallyWithoutNetwork() {
        assertEquals(biliAidToBvid(123), resolveVideoLink("https://www.bilibili.com/video/av123") {
            error("network must not run")
        })
        assertEquals(biliAidToBvid(123), resolveVideoLink("m.bilibili.com/video/av123?spm=x") {
            error("network must not run")
        })
    }

    @Test fun schemeLessAndAlternateShortHostsStillFollowRedirects() {
        val urls = mutableListOf<String>()
        val result = resolveVideoLink("bili2233.cn/AbC") { url ->
            urls += url.toString()
            Connection(url, "https://www.bilibili.com/video/$bv")
        }
        assertEquals(bv, result)
        assertEquals(listOf("https://bili2233.cn/AbC"), urls)
    }

    @Test fun relativeRedirectsUseTheSameFourHopRulesHeadersAndTimeouts() {
        val urls = mutableListOf<String>(); val connections = mutableListOf<Connection>()
        val result = resolveVideoLink("https://b23.tv/start") { url ->
            urls += url.toString()
            Connection(url, if (urls.size == 1) "/second" else "https://www.bilibili.com/video/bv" + bv.substring(2)).also(connections::add)
        }
        assertEquals(bv, result); assertEquals(listOf("https://b23.tv/start", "https://b23.tv/second"), urls)
        assertTrue(connections.all { !it.instanceFollowRedirects && it.connectTimeout == 5000 && it.readTimeout == 5000 })
        assertTrue(connections.all { it.getRequestProperty("User-Agent") == "Mozilla/5.0" && it.disconnected && it.connectedCount == 1 })
    }
    @Test fun blankLocationKeepsTheOriginalFourAttemptsWithoutGuessingAnotherPath() {
        val connections = mutableListOf<Connection>()
        assertNull(resolveVideoLink("https://b23.tv/start") { Connection(it, null).also(connections::add) })
        assertEquals(4, connections.size); assertTrue(connections.all { it.url.toString() == "https://b23.tv/start" && it.disconnected })
    }
    @Test fun redirectBudgetAndOrdinaryFailureKeepNullFallback() {
        var calls = 0
        assertNull(resolveVideoLink("https://b23.tv/start") { Connection(it, "/next${++calls}") }); assertEquals(4, calls)
        assertNull(resolveVideoLink("https://b23.tv/start") { throw IOException("offline") })
    }
}
