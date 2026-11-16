package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.model.feed.RecommendationMemory
import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class RecommendationRankingPolicyTest {
    private val time = 20 * RecommendationRankingPolicy.DAY
    private fun video(id: Int, up: Long = id.toLong()) = VideoItem(
        "v$id", id.toLong(), "", "", "", "", up, 0, 0, 60, 0)
    private fun rank(items: List<VideoItem>, memory: List<RecommendationMemory> = emptyList()) =
        RecommendationRankingPolicy.rank(items, memory, emptyList(), emptyList(), time)

    @Test fun coldStartWithDiverseCandidatesPreservesOfficialOrder() {
        val items = (1..20).map { video(it) }
        assertEquals(items, rank(items))
    }
    @Test fun duplicateBvIsEmittedOnce() {
        assertEquals(listOf(video(1), video(2)), rank(listOf(video(1), video(1), video(2))))
    }
    @Test fun diversitySeparatesAdjacentCreatorWhenPossible() {
        val items = listOf(video(1), video(2, 1), video(3), video(4))
        val result = rank(items)
        assertTrue(result.indexOf(video(3)) < result.indexOf(video(2, 1)))
    }
    @Test fun singleCreatorNeverProducesEmptyOrLostItems() {
        val items = (1..50).map { video(it, 1) }
        assertEquals(items.toSet(), rank(items).toSet())
    }
    @Test fun everyAdjustmentHonorsBothDirectionsOfShiftBound() {
        val items = (1..120).map { video(it, (it % 7).toLong()) }
        val memory = items.map { RecommendationMemory(it.bvid, it.ownerMid, "", time, time, 0, 1.0, time) }
        val result = rank(items, memory)
        result.forEachIndexed { index, value -> assertTrue(kotlin.math.abs(items.indexOf(value) - index) <= 3) }
        assertEquals(result, rank(items, memory))
    }
    @Test fun exposureReducesRankWithoutBecomingNegativeInterest() {
        val items = (1..20).map { video(it) }
        val memory = listOf(RecommendationMemory("v5", 5, "", time, time))
        assertTrue(rank(items, memory).indexOf(video(5)) > items.indexOf(video(5)))
    }
    @Test fun expiredExposureDoesNotChangeOrder() {
        val items = (1..20).map { video(it) }
        val memory = listOf(RecommendationMemory("v5", 5, "", time, time - 2 * RecommendationRankingPolicy.DAY))
        assertEquals(items, rank(items, memory))
    }
    @Test fun unknownCreatorDoesNotCountAsSameCreator() {
        val items = (1..20).map { video(it, 0) }
        assertEquals(items, rank(items))
    }
    @Test fun repeatedReplaysOfOneVideoHaveSmallBoundedInfluence() {
        val items = (1..20).map { video(it) }
        val memory = listOf(RecommendationMemory("seen", 7, "", time, satisfaction = 1.0, watchedAt = time))
        val result = rank(items, memory)
        assertTrue(kotlin.math.abs(result.indexOf(video(7)) - 6) <= 3)
    }
}
