package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.model.video.VideoItem
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpaceInfoMapperTest {
    private fun data(label: String): JSONObject = javaClass.classLoader!!
        .getResourceAsStream("json/space-$label-2283296.json")!!.bufferedReader().use {
            val root = JSONObject(it.readText()); assertEquals(0, root.getInt("code")); root.getJSONObject("data")
        }
    private fun payloads() = SpacePayloadsDto(data("info"), data("stat"), null, data("nav"), data("app"))
    private fun map(payloads: SpacePayloadsDto, likes: Long = 0) = mapSpaceInfo(payloads, likes) { error("no web vlist fixture") }

    @Test fun realSuccessfulResponsesKeepIdentityCountersAndOfficialFields() {
        val result = map(payloads()); val profile = result.profile
        assertEquals(2283296L, profile.mid); assertEquals("小猪P_", profile.name); assertEquals(6, profile.level)
        assertEquals(759601L, profile.follower); assertEquals(964L, profile.following); assertEquals(515, result.videoCount)
        assertEquals("bilibili 知名UP主", profile.officialTitle); assertEquals("", profile.officialDesc)
        assertEquals(0, profile.officialType); assertEquals(0.0, profile.coins, 0.0)
        assertTrue(result.videos.isEmpty()); assertFalse(result.isFollowed)
        assertTrue(profile.face.startsWith("https://")); assertTrue(profile.face.endsWith("@200w_200h_1c.webp"))
    }
    @Test fun realAppVipUsesLegacyVipStatusAndVipTypeFields() {
        val p = payloads(); val vip = p.appSpace!!.getJSONObject("card").getJSONObject("vip")
        assertFalse(vip.has("status")); assertFalse(vip.has("type"))
        val result = map(p).profile
        assertTrue(result.vip); assertEquals("十年大会员", result.vipLabel); assertEquals(426L, result.chargingCount)
    }
    @Test fun realBannerBranchesPreferAppDressingAndKeepFallbackOrder() {
        val p = payloads(); val profile = map(p).profile
        // App 空间装扮图优先；网页版超宽 top_photo 排在后面兜底，避免通栏头图只显示一角。
        assertEquals("https://i2.hdslb.com/bfs/space/dfb53c567f1c794cc68d89bdc11c060e3f157c54.jpg", profile.banner)
        assertEquals(listOf(
            "https://i0.hdslb.com/bfs/space/dfb53c567f1c794cc68d89bdc11c060e3f157c54.jpg",
            "https://i0.hdslb.com/bfs/space/6298c9dc83ddeca48968b384ad6f3dd1644f664e.jpg"), profile.bannerFallbacks)
    }
    @Test fun realAbsentMedalAndExperienceUseOriginalDefaults() {
        val p = map(payloads()).profile
        assertEquals("", p.fansMedalName); assertEquals(0, p.fansMedalLevel); assertEquals(0, p.fansMedalColor)
        assertEquals(0L, p.levelExp); assertEquals(0L, p.nextLevelExp); assertEquals(0L, p.likes)
        assertEquals(0, data("upstat").length())
    }
    @Test fun networkResolvedLikesArePassedWithoutAnotherRequestOrReinterpretation() {
        assertEquals(12345L, map(payloads(), 12345).profile.likes)
    }
    @Test fun missingOptionalResponsesKeepWebProfileAndDefaults() {
        val result = map(SpacePayloadsDto(data("info"), null, null, null, null))
        assertEquals("小猪P_", result.profile.name); assertFalse(result.profile.vip)
        assertEquals("", result.profile.vipLabel); assertEquals(0L, result.profile.chargingCount)
        assertEquals(0L, result.profile.follower); assertEquals(0L, result.profile.following)
        assertEquals(0, result.videoCount); assertTrue(result.videos.isEmpty())
    }
    @Test fun positiveModernVipFieldsTakePrecedenceOverLegacyOnes() {
        val p = payloads(); val vip = p.appSpace!!.getJSONObject("card").getJSONObject("vip")
        vip.put("status", 2); vip.put("vipStatus", 1); vip.put("type", 1); vip.put("vipType", 2)
        vip.getJSONObject("label").put("text", "").put("label_theme", "")
        val result = map(p).profile; assertFalse(result.vip); assertEquals("", result.vipLabel)
    }
    @Test fun vipThemeFallbackPriorityAndCaseRemainUnchanged() {
        val expected = linkedMapOf("hundred_ten_annual" to "百年大会员", "TEN_YEAR" to "十年大会员",
            "ten" to "十年大会员", "ANNUAL" to "年度大会员", "other" to "年度大会员")
        for ((theme, label) in expected) {
            val p = payloads(); p.appSpace!!.getJSONObject("card").getJSONObject("vip")
                .getJSONObject("label").put("text", "").put("label_theme", theme)
            assertEquals(theme, label, map(p).profile.vipLabel)
        }
    }
    @Test fun zeroOrNegativeChargeFallsBackToActualReturnedListLength() {
        val p = payloads(); val elec = p.appSpace!!.getJSONObject("elec")
        val count = elec.getJSONArray("list").length().toLong(); assertEquals(4L, count)
        for (total in listOf(0, -1)) { elec.put("total", total); assertEquals(count, readSpaceCharge(p.appSpace)) }
        elec.remove("list"); assertEquals(0L, readSpaceCharge(p.appSpace)); assertEquals(0L, readSpaceCharge(null))
    }
    @Test fun syntheticVlistProbePreservesOrderNullFilteringAndRawCountFallback() {
        val items = JSONArray().put(JSONObject().put("id", "first")).put(JSONObject().put("id", "skip"))
            .put(JSONObject().put("id", "first"))
        val p = payloads().copy(videos = JSONObject().put("list", JSONObject().put("vlist", items)), nav = null)
        val seen = mutableListOf<String>()
        val video = VideoItem("BVprobe", 1, "title", "", "", "", 1, 0, 0, 1, 0)
        val result = mapSpaceInfo(p, 0) { seen += it.getString("id"); if (it.getString("id") == "skip") null else video }
        assertEquals(listOf("first", "skip", "first"), seen); assertEquals(listOf(video, video), result.videos)
        assertEquals(3, result.videoCount)
    }
}
