package com.aritxonly.bdfun.infra.bili.mapper

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 样本来自真实响应（2026-11-14 匿名拉取）：
 * - tabs: /pgc/season/rank/tabs?index_id=1
 * - board: /pgc/season/rank/v2/list?oid={season_type:1,style_id:70}&rank_id=125
 * 原始完整样本见 D:\work\_apicollect\samples\。
 */
class PgcRankMapperTest {

    @Test
    fun `解析榜单目录并保留 oid 原样`() {
        val tabs = mapRankTabs(JSONArray(TABS_JSON))

        assertEquals(2, tabs.size)
        assertEquals(124L, tabs[0].rankId)
        assertEquals("热门榜", tabs[0].title)
        // oid 不是合法 JSON，必须逐字保留，编码由请求层做。
        assertEquals("{season_type:1,style_id:0}", tabs[0].oid)
        assertEquals(125L, tabs[1].rankId)
        assertEquals("{season_type:1,style_id:70}", tabs[1].oid)
    }

    @Test
    fun `解析榜单内容并映射卡片字段`() {
        val board = mapRankBoard(JSONObject(BOARD_JSON))

        assertEquals("根据内容的热度排名，每小时更新一次", board.note)
        assertEquals(2, board.entries.size)

        val first = board.entries[0]
        assertEquals(1, first.rank)
        assertEquals(348190L, first.seasonId)
        assertEquals("海绵宝宝 第十二季 中文配音", first.title)
        assertEquals("准备好了吗孩子们", first.subtitle)
        assertEquals("更新至第18话", first.indexShow)
        assertEquals("荣登榜首", first.tag)
        assertEquals("", first.rating) // 无评分时是空串，不能当异常
        assertTrue(first.cover.contains("@480w"))

        val second = board.entries[1]
        assertEquals(2, second.rank)
        assertEquals("银魂", second.title)
        assertEquals("全200话", second.indexShow)
        assertEquals("9.9", second.rating)
    }

    @Test
    fun `缺 new_ep 与空 season_vo_list 都不抛异常`() {
        val noEp = mapRankBoard(JSONObject("""{"note":"","season_vo_list":[{"rank":3,"season_id":1,"title":"x"}]}"""))
        assertEquals("", noEp.entries.single().indexShow)

        val empty = mapRankBoard(JSONObject("""{"note":"","season_vo_list":[]}"""))
        assertEquals(0, empty.entries.size)

        // season_id 缺失的项直接丢弃，不产生 seasonId=0 的脏数据。
        val dirty = mapRankBoard(JSONObject("""{"note":"","season_vo_list":[{"rank":1,"title":"bad"}]}"""))
        assertEquals(0, dirty.entries.size)
    }

    private companion object {
        const val TABS_JSON = """
        [
          {"oid":"{season_type:1,style_id:0}","rank_id":124,"rank_title":"热门榜"},
          {"oid":"{season_type:1,style_id:70}","rank_id":125,"rank_title":"搞笑榜"}
        ]
        """

        const val BOARD_JSON = """
        {
          "color_control": 1,
          "note": "根据内容的热度排名，每小时更新一次",
          "season_vo_list": [
            {
              "rank": 1,
              "season_id": 348190,
              "title": "海绵宝宝 第十二季 中文配音",
              "cover": "https://i0.hdslb.com/bfs/bangumi/image/666e8d57768a22d3f7ea5d53ef7b501fdde4833f.png",
              "subtitle": "准备好了吗孩子们",
              "rating": "",
              "season_style": "番剧·少儿·搞笑",
              "season_tag": "荣登榜首",
              "new_ep": {"cover": "", "ep_id": 7515973, "index_show": "更新至第18话"},
              "type": "PGC"
            },
            {
              "rank": 2,
              "season_id": 38157,
              "title": "银魂",
              "cover": "http://i0.hdslb.com/bfs/bangumi/image/087b862b772ee4e644478a36c757a26db476193d.png",
              "subtitle": "最无厘头的热血动画",
              "rating": "9.9",
              "season_style": "番剧·漫画改·热血",
              "season_tag": "",
              "new_ep": {"cover": "", "ep_id": 394945, "index_show": "全200话"},
              "type": "PGC"
            }
          ]
        }
        """
    }
}
