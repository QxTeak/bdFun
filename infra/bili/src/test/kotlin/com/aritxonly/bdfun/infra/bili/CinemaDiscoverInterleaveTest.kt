package com.aritxonly.bdfun.infra.bili

import com.aritxonly.bdfun.core.model.pgc.BangumiCard
import org.junit.Assert.assertEquals
import org.junit.Test

class CinemaDiscoverInterleaveTest {
    private fun card(id: Long) = BangumiCard(seasonId = id, title = "s$id", cover = "")

    @Test
    fun `round robin keeps one card per type before the second round`() {
        val pages = listOf(
            listOf(card(1), card(2)),
            listOf(card(3), card(4)),
            listOf(card(5)),
        )

        assertEquals(listOf(1L, 3L, 5L, 2L, 4L), interleaveCinemaCards(pages).map { it.seasonId })
    }

    @Test
    fun `duplicate season ids keep their first occurrence`() {
        val pages = listOf(
            listOf(card(1), card(2)),
            listOf(card(2), card(3)),
        )

        assertEquals(listOf(1L, 2L, 3L), interleaveCinemaCards(pages).map { it.seasonId })
    }

    @Test
    fun `empty types do not interrupt the other pages`() {
        val pages = listOf(
            emptyList(),
            listOf(card(7), card(8)),
            emptyList(),
            listOf(card(9)),
        )

        assertEquals(listOf(7L, 9L, 8L), interleaveCinemaCards(pages).map { it.seasonId })
        assertEquals(emptyList<BangumiCard>(), interleaveCinemaCards(emptyList()))
    }
}
