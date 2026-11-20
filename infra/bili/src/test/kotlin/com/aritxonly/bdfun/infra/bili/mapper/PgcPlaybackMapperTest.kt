package com.aritxonly.bdfun.infra.bili.mapper

import com.aritxonly.bdfun.core.domain.videoCommentTarget
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 样本来源：2026-11-19 匿名请求 `https://api.bilibili.com/pgc/view/web/season?season_id=43161`，
 * 只截取前 2 集正片 + 第 1 个 section（共 9 集 / 7 个 section），字段原样，未改写含义。
 */
class PgcPlaybackMapperTest {
    private fun sample(): JSONObject = javaClass.classLoader!!
        .getResourceAsStream("json/pgc-season-43161.json")!!
        .bufferedReader().use { JSONObject(it.readText()).getJSONObject("result") }

    private fun season() = mapBangumiSeason(sample())

    @Test fun seasonReadsStatOwnerAndSections() {
        val season = season()
        assertEquals(43161L, season.seasonId)
        assertEquals("不止考古·我与三星堆", season.title)
        assertEquals(3, season.type)
        assertEquals("9.7", season.rating)
        assertEquals(236669553L, season.stat.play)
        assertEquals(138322L, season.stat.danmaku)
        assertEquals(9136912L, season.stat.like)
        assertEquals(244166L, season.stat.coin)
        assertEquals(598786L, season.stat.favorite)
        assertEquals(59904L, season.stat.reply)
        assertEquals(7584632L, season.up.mid)
        assertEquals("哔哩哔哩纪录片", season.up.name)
        assertFalse(season.userStatus.followed)
        assertEquals(0L, season.userStatus.lastEpId)
        // 正片 2 集 + section 1 集；section 是真实 key（不是 sections）。
        assertEquals(3, season.episodes.size)
        assertEquals("正片", season.episodes[0].sectionTitle)
        assertEquals(2, season.episodes.count { it.sectionTitle == "正片" })
    }

    @Test fun episodeReadsPgcIdentityAndDisplayName() {
        val first = season().episodes.first()
        assertEquals(689147L, first.epId)
        assertEquals(901628511L, first.aid)
        assertEquals(1146763843L, first.cid)
        assertEquals("BV1KN4y1w784", first.bvid)
        assertEquals("微痕之下", first.longTitle)
        assertTrue(first.showTitle.isNotBlank())
        assertEquals(first.showTitle, first.displayName)
        assertEquals(2747000L, first.durationMs)
        assertEquals(1666260000L, first.pubTime)
    }

    @Test fun detailMapsEpisodeToVideoPageAndCommentIdentity() {
        val season = season()
        val episode = season.episodes.first()
        val detail = mapPgcVideoDetail(season, episode) { it }
        assertTrue(detail.isPgc)
        assertEquals(689147L, detail.epId)
        assertEquals(43161L, detail.seasonId)
        assertEquals(episode.cid, detail.cid)
        assertEquals(2747, detail.duration)
        assertEquals("纪录片", detail.tname)
        // PGC 没有分 P，选集统一走合集列表。
        assertTrue(detail.pages.isEmpty())
        assertEquals(3, detail.episodes.size)
        assertEquals(689147L, detail.episodes.first().epId)
        // 评论区身份是单集 aid（用 ep_id 当 oid 会返回 0 条）。
        assertEquals(detail.aid.toString(), videoCommentTarget(detail).oid)
        assertEquals(1, videoCommentTarget(detail).type)
        // 封面不能用列表缩略图（@320w）当详情大图。
        assertTrue(detail.cover.endsWith("@1200w_675h_1c.webp"))
    }

    @Test fun missingSectionKeyStillKeepsMainEpisodes() {
        val raw = JSONObject(sample().toString())
        raw.remove("section")
        assertEquals(2, mapBangumiSeason(raw).episodes.size)
    }
}
