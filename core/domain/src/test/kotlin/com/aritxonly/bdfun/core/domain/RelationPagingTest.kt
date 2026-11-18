package com.aritxonly.bdfun.core.domain



import org.junit.Assert.*
import org.junit.Test

class RelationPagingTest {
    @Test fun requestMatchesHyperPiliForEachListAndOrder() {
        val recent = relationListParams(42, false, 2, false)
        assertEquals(mapOf("vmid" to "42", "pn" to "2", "ps" to "20", "order" to "desc", "order_type" to ""), recent)
        assertEquals("attention", relationListParams(42, false, 1, true)["order_type"])
        assertFalse(relationListParams(42, true, 1, true).containsKey("order_type"))
    }
    @Test fun fullLastPageAndPrivatePartialPagesDoNotLoop() {
        assertNull(relationNextPage(2, 20, 40))
        assertNull(relationNextPage(1, 3, 999))
        assertNull(relationNextPage(3, 0, 999))
        assertEquals(2, relationNextPage(1, 20, 41))
        assertEquals(2, relationNextPage(1, 20, 0))
    }
    @Test fun appDressingLeadsAndWebBannerIsOnlyAFallback() {
        // App 装扮图必须是头图；网页版超宽 top_photo 只在没有装扮图时兜底。
        assertEquals(listOf("app-day", "app-night", "custom"), orderedSpaceBanners("custom", "app-day", "app-night"))
        assertEquals(listOf("custom"), orderedSpaceBanners("custom", "", ""))
    }
    @Test fun relativeWebPathsUseCdnAndDefaultStaysLast() {
        val default = "bfs/space/cb1c3ef50e22b6096fde67febe863494caefebad.png"
        assertEquals(listOf("app-custom", "https://i0.hdslb.com/$default"),
            orderedSpaceBanners(default, "app-custom", ""))
        assertEquals(listOf("https://i0.hdslb.com/$default"), orderedSpaceBanners(default, "", ""))
        assertEquals("https://i0.hdslb.com/bfs/space/custom.png", normalizeSpaceBanner("/bfs/space/custom.png"))
        assertEquals("https://i1.hdslb.com/image.png", normalizeSpaceBanner("//i1.hdslb.com/image.png"))
    }
}
