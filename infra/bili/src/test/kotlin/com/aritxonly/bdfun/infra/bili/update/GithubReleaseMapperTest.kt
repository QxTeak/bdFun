package com.aritxonly.bdfun.infra.bili.update

import com.aritxonly.bdfun.core.domain.update.latestRelease
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test

class GithubReleaseMapperTest {
    private fun fixture() = JSONArray(javaClass.getResource("/update/github-releases.json")!!.readText())
    @Test fun realReleasesMapCurrentNotesAndExcludeDebugApk() {
        val array = fixture()
        val current = githubRelease(array.getJSONObject(0))!!
        assertEquals(500, current.version.code)
        assertTrue(current.notes.isNotBlank())
        assertTrue(current.prerelease) // GitHub 旧发布标志为 false，但 preview 标签仍属测试渠道。
        val older = githubRelease(array.getJSONObject(1))!!
        assertTrue(older.apk!!.endsWith("bdFun-preview-4.3-release.apk"))
        assertFalse(older.apk!!.contains("debug"))
    }
    @Test fun unknownVersionsRemainHistoryOnlyAndDraftsAreHidden() {
        val array = fixture()
        val legacy = githubRelease(array.getJSONObject(3))!!
        assertEquals("preview-3.9.4", legacy.version.name)
        assertNull(latestRelease(listOf(legacy), true))
        assertNull(latestRelease(listOf(githubRelease(array.getJSONObject(0))!!), false))
        assertNull(githubRelease(array.getJSONObject(0).put("draft", true)))
    }
}
