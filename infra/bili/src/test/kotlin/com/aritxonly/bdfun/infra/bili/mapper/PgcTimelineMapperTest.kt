package com.aritxonly.bdfun.infra.bili.mapper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class PgcTimelineMapperTest {

    // 样本按 2026-11-15 实际抓取的 /pgc/web/timeline 结构截取，不是字段猜测。
    private val payload = JSONObject(
        """
        {
          "result": [
            {
              "date": "11-15",
              "episodes": [
                {
                  "cover": "https://i0.hdslb.com/bfs/bangumi/a.png",
                  "ep_cover": "https://i0.hdslb.com/bfs/archive/a.png",
                  "pub_index": "第3话",
                  "pub_ts": 1794300000,
                  "season_id": 1001,
                  "title": "新番 A",
                  "follows": "12.3万"
                },
                {
                  "cover": "https://i0.hdslb.com/bfs/bangumi/b.png",
                  "pub_index": "第8话",
                  "pub_ts": 1794200000,
                  "season_id": 1002,
                  "title": "新番 B",
                  "follows": "-"
                }
              ]
            },
            {
              "date": "11-16",
              "episodes": [
                {
                  "cover": "https://i0.hdslb.com/bfs/bangumi/c.png",
                  "pub_index": "第4话",
                  "pub_ts": 1794400000,
                  "season_id": 1001,
                  "title": "新番 A",
                  "follows": "12.3万"
                },
                {
                  "cover": "https://i0.hdslb.com/bfs/bangumi/d.png",
                  "pub_index": "第1话",
                  "pub_ts": 1794100000,
                  "season_id": 1003,
                  "title": "新番 C",
                  "follows": "5.1万"
                },
                {
                  "cover": "https://i0.hdslb.com/bfs/archive/e.png",
                  "pub_index": "第9话",
                  "pub_ts": 1794255000,
                  "season_id": 1004,
                  "title": "新番 A 中文配音",
                  "follows": "12.3万"
                }
              ]
            }
          ]
        }
        """.trimIndent(),
    )

    @Test
    fun keepsClosestEpisodePerSeasonAndSortsByDistance() {
        val now = 1794250000L
        val cards = mapPgcTimeline(payload, now)
        // 「新番 A」和「新番 A 中文配音」是同一部番的两个 season，头图只留离当前最近的一条。
        assertEquals(listOf(1004L, 1002L, 1003L), cards.map { it.seasonId })
        // 同一部番留离当前时间最近的一集，而不是后一天的第4话；头图不展示集数，简介由仓储补 season 详情。
        assertEquals("", cards.first().indexShow)
        assertEquals("12.3万", cards.first().followText)
        // ep_cover 优先作横版头图，没有时才退回 cover。
        assertTrue(cards.first().cover.contains("archive/e.png"))
        assertTrue(cards[1].cover.contains("bangumi/b.png"))
        assertEquals("", cards[1].followText)
    }

    @Test
    fun emptyTimelineMapsToEmpty() {
        assertEquals(emptyList<Any>(), mapPgcTimeline(JSONObject(), 0L))
    }
}
