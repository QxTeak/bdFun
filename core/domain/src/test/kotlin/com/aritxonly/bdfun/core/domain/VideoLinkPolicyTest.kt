package com.aritxonly.bdfun.core.domain

import com.aritxonly.bdfun.core.domain.videoTitleTarget
import com.aritxonly.bdfun.core.domain.videoLinkTokens
import com.aritxonly.bdfun.core.domain.shortVideoUrl
import com.aritxonly.bdfun.core.domain.recognizesVideoLink
import com.aritxonly.bdfun.core.domain.isVideoLinkToken
import com.aritxonly.bdfun.core.domain.directVideoBvid
import com.aritxonly.bdfun.core.domain.normalizedShortVideoUrl
import org.junit.Assert.*
import org.junit.Test

class VideoLinkPolicyTest {
    private val bv = "BV1orW46aEba"
    @Test fun tokensKeepOrderDistinctnessAndTrimTheOriginalPunctuation() {
        val url = "https://www.bilibili.com/video/$bv?p=2"
        assertEquals(listOf(bv, "av123", url, "https://b23.tv/Ab12"),
            videoLinkTokens("$bv $bv av123 $url）。 https://b23.tv/Ab12，"))
    }
    @Test fun bareIdsKeepAsciiBoundaryAndMinimumAidLength() {
        assertTrue(videoLinkTokens("x$bv 1$bv av12 xav123").isEmpty())
        assertEquals(listOf(bv, "av123"), videoLinkTokens("_$bv（av123）"))
        assertTrue(isVideoLinkToken("https://b23.tv/Ab12"))
        assertFalse(isVideoLinkToken("https://b23.tv/Ab12?x=1"))
    }
    @Test fun titleTargetsPreserveTheDifferentBareAndUrlRules() {
        assertEquals(VideoTitleTarget.Bvid(bv), videoTitleTarget("bv" + bv.substring(2)))
        assertEquals(VideoTitleTarget.Bvid("bv" + bv.substring(2)), videoTitleTarget("https://www.bilibili.com/video/bv" + bv.substring(2)))
        assertEquals(VideoTitleTarget.Aid(123), videoTitleTarget("AV123"))
        assertEquals(VideoTitleTarget.Aid(123), videoTitleTarget("https://www.bilibili.com/video/av123"))
        assertEquals(VideoTitleTarget.Short("HTTPS://B23.TV/ABC"), videoTitleTarget("HTTPS://B23.TV/ABC"))
        assertEquals(VideoTitleTarget.Unsupported, videoTitleTarget("av999999999999999999999999"))
    }

    @Test fun schemeLessAndAlternateShortHostsAreRecognized() {
        assertEquals(bv, directVideoBvid("bilibili.com/video/$bv?p=3"))
        assertTrue(recognizesVideoLink("bilibili.com/video/$bv"))
        assertTrue(recognizesVideoLink("https://bili2233.cn/AbC123"))
        assertEquals("https://b23.tv/Ab12", normalizedShortVideoUrl("b23.tv/Ab12"))
        assertEquals(VideoTitleTarget.Aid(456), videoTitleTarget("m.bilibili.com/video/av456"))
    }
    @Test fun activityRecognitionDoesNotPretendToSupportBareAidResolution() {
        assertTrue(recognizesVideoLink("x$bv"))
        assertFalse(recognizesVideoLink("av123"))
        assertTrue(recognizesVideoLink("https://www.bilibili.com/video/av123"))
        assertNull(directVideoBvid("av123"))
        assertEquals(bv, directVideoBvid("before bv" + bv.substring(2) + " after"))
        assertEquals("HTTPS://B23.TV/Ab1", shortVideoUrl("url HTTPS://B23.TV/Ab1!"))
    }
}
