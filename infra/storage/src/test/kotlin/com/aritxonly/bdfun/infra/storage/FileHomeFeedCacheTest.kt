package com.aritxonly.bdfun.infra.storage

import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.model.video.VideoItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files

class FileHomeFeedCacheTest {
    @Test fun roundTripIsBoundedAndAccountsUseSeparateFiles() = runBlocking {
        val dir = Files.createTempDirectory("home-cache").toFile()
        try {
            val first = FileHomeFeedCache(java.io.File(dir, "1.json"))
            val second = FileHomeFeedCache(java.io.File(dir, "2.json"))
            val items = (1..30).map { VideoItem("BV$it", it.toLong(), "标题", "cover", "UP", "face", 1, 2, 3, 4, 5) }
            assertEquals(Outcome.Success(Unit), first.write(items))
            assertEquals(Outcome.Success(items.take(24)), first.read())
            assertEquals(Outcome.Success(emptyList<VideoItem>()), second.read())
            java.io.File(dir, "1.json").writeText("invalid")
            assertTrue(first.read() is Outcome.Failure)
        } finally { dir.deleteRecursively() }
    }
}
