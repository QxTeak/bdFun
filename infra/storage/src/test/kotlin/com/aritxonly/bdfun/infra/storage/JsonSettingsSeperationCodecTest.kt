package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class JsonSettingsSeperationCodecTest {
    private fun index(): SettingsIndex = SettingsIndex((JsonSettingsIndexCodec().decode(
        File("../../app/src/main/assets/settings-index.json").readText()) as SettingsIndexImport.Success).categories)
    @Test fun bundledSeperationCoversAllSixPagesInFourGroups() {
        val decoded = JsonSettingsSeperationCodec().decode(File("../../app/src/main/assets/settings-seperation.json").readText(), index())
        assertTrue(decoded.toString(), decoded is SettingsSeperationImport.Success)
        val groups = (decoded as SettingsSeperationImport.Success).index.groups
        assertEquals(4, groups.size)
        assertEquals(6, groups.sumOf { it.entries.size })
    }
    @Test fun malformedOrPartialImportsAreRejectedAtomically() {
        val codec = JsonSettingsSeperationCodec()
        listOf("broken", "[]", "[{}]", "[{\"id\":\"x\",\"title\":\"x\",\"entries\":[]}]").forEach {
            assertTrue(codec.decode(it, index()) is SettingsSeperationImport.Invalid)
        }
    }
}
