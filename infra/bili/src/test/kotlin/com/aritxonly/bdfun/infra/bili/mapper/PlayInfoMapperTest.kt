package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.infra.bili.dto.PlayInfoDto
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PlayInfoMapperTest {
    private fun realData(): JSONObject = javaClass.classLoader!!
        .getResourceAsStream("json/playurl-anonymous.json")!!.bufferedReader()
        .use { JSONObject(it.readText()).getJSONObject("data") }
    private fun original(dto: PlayInfoDto, codec: String = "auto") =
        mapPlayInfo(dto, 99, codec) { url, _ -> url }

    @Test fun realAnonymousResponseRetainsAllAvailableQualitiesAndAudioIdentity() {
        val dto = readPlayInfo(realData())
        val info = original(dto)
        assertEquals(listOf(32, 16), info.qualities.map { it.id })
        assertTrue(info.qualities.all { it.codec.startsWith("avc") })
        assertEquals(852, info.qualities.first().width)
        assertEquals(480, info.qualities.first().height)
        assertEquals("23.986", info.qualities.first().frameRate)
        assertEquals(dto.audios.single { it.id == 30280 }.url, info.rawAudioUrl)
        assertEquals(info.rawAudioUrl, info.audioUrl)
        assertEquals(98452L, info.durationMs)
        assertEquals(listOf(64, 32, 16), info.acceptQuality)
        assertNull(info.fallbackUrl)
        // 真实样本带 SegmentBase/segment_base：范围必须解析出来，本地 MPD 才拼得动。
        assertNotNull(info.qualities.first().initRange)
        assertNotNull(info.qualities.first().indexRange)
        assertTrue(info.qualities.first().bandwidth > 0)
        assertNotNull(info.audioInitRange)
        assertNotNull(info.audioIndexRange)
        assertEquals("mp4a.40.2", info.audioCodec)
    }

    @Test fun segmentBaseAliasesAreReadForBothCases() {
        val dto = readPlayInfo(
            JSONObject(
                """{"timelength":100000,"dash":{"video":[
                   {"id":80,"baseUrl":"v","bandwidth":1000,"width":1920,"height":1080,
                    "SegmentBase":{"Initialization":"0-10","indexRange":"11-20"}}],
                   "audio":[{"id":30280,"baseUrl":"a","bandwidth":100,
                    "segment_base":{"initialization":"0-5","index_range":"6-9"}}]}}""",
            ),
        )
        assertEquals("0-10", dto.videos.single().initRange)
        assertEquals("11-20", dto.videos.single().indexRange)
        assertEquals("0-5", dto.audios.single().initRange)
        assertEquals("6-9", dto.audios.single().indexRange)
    }

    @Test fun explicitCodecAndCdnMappingKeepRawUrlsAndOriginalRewriteOrder() {
        val dto = readPlayInfo(realData())
        val calls = mutableListOf<Pair<String, List<String>>>()
        val info = mapPlayInfo(dto, 99, "hevc") { url, backups ->
            calls += url to backups
            "mapped:$url"
        }
        val selected = listOf(32, 16).map { id -> dto.videos.single { it.id == id && it.codec.startsWith("hvc") } }
        assertEquals(selected.map { it.url }, info.qualities.map { it.rawVideoUrl })
        assertEquals(selected.map { "mapped:${it.url}" }, info.qualities.map { it.videoUrl })
        val audio = dto.audios.single { it.id == 30280 }
        assertEquals(listOf(audio) + selected, calls.map { call ->
            (dto.audios + dto.videos).single { it.url == call.first }
        })
        assertEquals((listOf(audio) + selected).map { it.backups }, calls.map { it.second })
    }

    @Test fun missingFieldsKeepEmptyListsNullUrlsAndLegacyDurationFallback() {
        val dto = readPlayInfo(JSONObject())
        val info = mapPlayInfo(dto, 99, "auto") { _, _ -> error("nothing to rewrite") }
        assertTrue(info.qualities.isEmpty())
        assertTrue(info.acceptQuality.isEmpty())
        assertNull(info.audioUrl)
        assertNull(info.rawAudioUrl)
        assertNull(info.fallbackUrl)
        assertEquals(99000L, info.durationMs)
    }

    @Test fun snakeCaseAliasesAndFpsAreReadWithoutDroppingEmptyRepresentations() {
        val dto = readPlayInfo(JSONObject("""{"dash":{"video":[{},
            {"id":80,"baseUrl":"","base_url":"video","backup_url":["","backup"],"fps":"60"}]}}"""))
        assertEquals(2, dto.videos.size)
        assertEquals("", dto.videos.first().url)
        assertEquals(listOf("backup"), dto.videos.last().backups)
        val info = original(dto)
        assertEquals(listOf(80), info.qualities.map { it.id })
        assertEquals("video", info.qualities.single().rawVideoUrl)
        assertEquals("60", info.qualities.single().frameRate)
    }

    @Test fun durlUsesOnlyFirstEntryAndKeepsAcceptOrderAndServerMilliseconds() {
        val dto = readPlayInfo(JSONObject("""{"timelength":1234,"durl":[{"url":"first"},{"url":"second"}],
            "accept_quality":[80,"invalid",16]}"""))
        val info = mapPlayInfo(dto, 99, "auto") { url, backups ->
            assertTrue(backups.isEmpty())
            "mapped:$url"
        }
        assertEquals("first", info.rawFallbackUrl)
        assertEquals("mapped:first", info.fallbackUrl)
        assertEquals(listOf(80, 0, 16), info.acceptQuality)
        assertEquals(1234L, info.durationMs)
    }

    @Test fun blankSelectedCodecIsSkippedAfterSelectionWithoutChoosingAnotherCodec() {
        val real = readPlayInfo(realData())
        val avc = real.videos.first { it.id == 32 && it.codec.startsWith("avc") }.copy(url = "")
        val other = real.videos.first { it.id == 32 && it.codec.startsWith("hvc") }
        val info = original(real.copy(videos = listOf(avc, other)))
        assertTrue(info.qualities.isEmpty())
        assertNotNull(info.audioUrl)
    }
}
