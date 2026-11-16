package com.aritxonly.bdfun.core.domain.update

import org.junit.Assert.*
import org.junit.Test

class AppVersionTest {
    @Test fun codesMatchReleaseContract() {
        assertEquals(500, AppVersion.parse("preview-5.0")?.code)
        assertEquals(10100, AppVersion.parse("1.1.0")?.code)
        assertEquals(9999999, AppVersion.parse("999.99.99")?.code)
        assertEquals(599, AppVersion.parse("vpreview-5.99")?.code)
        assertTrue(AppVersion.parse("1.0.0")!!.code > AppVersion.parse("preview-99.99")!!.code)
    }
    @Test fun invalidVersionsNeverBecomeUpdates() {
        listOf("preview-5.0.1", "1.100.0", "1.0", "01.0.0", "preview-05.0", "1.0.0-debug", "-1.0.0", "1000.0.0").forEach {
            assertNull(it, AppVersion.parse(it))
        }
    }
    @Test fun notesRemoveOnlySupportedFormatting() {
        assertEquals(listOf("新增 唱片控件", "优化 链接", "修复 Bug"),
            releaseNoteLines("- **新增** 唱片控件\\\n- 优化 [链接](https://example.com)\n- 修复 `Bug`"))
        assertEquals(emptyList<String>(), releaseNoteLines(" \n"))
    }
}
