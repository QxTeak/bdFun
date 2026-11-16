package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.model.feed.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class FileRecommendationStorageTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun missingFileDefaultsOffAndRoundTripPreservesEvidence() {
        val storage = FileRecommendationStorage(folder.root)
        assertFalse(storage.read().enabled)
        val archive = RecommendationArchive(true, mapOf("hashed-account" to listOf(
            RecommendationMemory("BV1", 1, "分类", 1000, 500, 700, 0.8, 900))))
        storage.write(archive)
        assertEquals(archive, storage.read())
        assertFalse(folder.root.resolve("recommendation_v1.tmp").exists())
    }
    @Test fun unknownVersionIsSafelyIgnored() {
        folder.root.resolve("recommendation_v1.json").writeText("""{"version":99,"enabled":true}""")
        assertEquals(RecommendationArchive(), FileRecommendationStorage(folder.root).read())
    }
    @Test fun clearingCurrentAccountDoesNotDeleteOtherAccounts() {
        val storage = FileRecommendationStorage(folder.root)
        val row = RecommendationMemory("BV1", 1, "", 1000)
        storage.write(RecommendationArchive(true, mapOf("a" to listOf(row), "b" to listOf(row))))
        storage.write(storage.read().let { it.copy(accounts = it.accounts - "a") })
        assertEquals(setOf("b"), storage.read().accounts.keys)
        assertTrue(storage.read().enabled)
    }
    @Test(expected = IllegalStateException::class) fun oversizedFilesAreNotReadIntoMemory() {
        folder.root.resolve("recommendation_v1.json").outputStream().use {
            it.write(ByteArray(FileRecommendationStorage.MAX_BYTES + 1))
        }
        FileRecommendationStorage(folder.root).read()
    }
}
