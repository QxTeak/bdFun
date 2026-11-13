package com.aritxonly.bdfun.feature.feed.application

import com.aritxonly.bdfun.core.model.video.*
import org.junit.Assert.*
import org.junit.Test

class InlineVideoMediaTest {
    private fun quality(id: Int, url: String = "q$id") = Quality(id, "", 1, 1, "", "", url)
    private fun info(vararg qualities: Quality, fallback: String? = "fallback", audio: String? = "audio") =
        PlayInfo(qualities.toList(), audio, fallback, 1, emptyList())

    @Test fun preferredQualityWinsEvenWhenNotFirst() {
        assertEquals(InlineVideoMedia("q32", "audio"), selectInlineVideoMedia(info(quality(80), quality(64), quality(32)), 32))
    }
    @Test fun missingPreferredUses64ThenLastServerEntryWithoutSorting() {
        assertEquals(InlineVideoMedia("q64", "audio"), selectInlineVideoMedia(info(quality(32), quality(64), quality(80)), 120))
        assertEquals(InlineVideoMedia("q80", "audio"), selectInlineVideoMedia(info(quality(32), quality(80)), 120))
    }
    @Test fun emptyQualitiesUseFallbackAndNullFallbackDoesNotPrepareSource() {
        assertEquals(InlineVideoMedia("fallback", "audio"), selectInlineVideoMedia(info(), 64))
        assertNull(selectInlineVideoMedia(info(fallback = null), 64))
    }
    @Test fun originalBlankVideoOrAudioValuesAreNotNormalizedBySelection() {
        assertEquals(InlineVideoMedia("", "  "), selectInlineVideoMedia(info(quality(64, ""), audio = "  "), 64))
        assertEquals(InlineVideoMedia("", null), selectInlineVideoMedia(info(fallback = "", audio = null), 64))
    }
}
