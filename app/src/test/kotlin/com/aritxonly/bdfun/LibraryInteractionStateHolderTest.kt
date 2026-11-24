package com.aritxonly.bdfun

import com.aritxonly.bdfun.core.model.library.HistoryItem
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.library.application.*
import com.aritxonly.bdfun.feature.settings.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.map
import org.junit.Assert.*
import org.junit.Test

class LibraryInteractionStateHolderTest {
    private class Fixture : AutoCloseable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settings = SettingsStateHolder.create(MemorySettingsPreferences(), TestHistoryCodec, scope)
        val holder = LibraryInteractionStateHolder(settings.general, settings.state.map { Unit }, scope, "library_0")
        override fun close() { holder.close(); scope.cancel() }
    }
    private fun video(id: Int, title: String = "猫咪", owner: String = "UP") = VideoItem("BV$id", id.toLong(), title, "", owner, "", 1, 0, 0, 1, 0)
    private fun entry(id: Int, title: String = "猫咪", author: String = "UP") = HistoryItem("BV$id", title, "", author, 0, 0, 10, aid = id.toLong())
    @Test fun backClosesTransferBeforeSearchAndBatchWithoutClearingTheirSelection() {
        Fixture().use { f ->
            val h=f.holder; h.toggleSearch(); h.toggleBatch(); h.toggleSelected("BV1")
            h.batchAction("move", 0, LibraryUiState())
            assertFalse(h.back()); assertNull(h.state.value.transferMode)
            assertTrue(h.state.value.searching); assertTrue(h.state.value.batchMode); assertEquals(setOf("BV1"), h.state.value.selected)
            assertFalse(h.back()); assertFalse(h.state.value.searching); assertFalse(h.state.value.batchMode)
            assertTrue(h.state.value.selected.isEmpty()); assertTrue(h.back())
        }
    }
    @Test fun pageChangeClearsOnlyBatchAndSelectionWhileSearchAndTransferRemain() {
        Fixture().use { f ->
            val h=f.holder; h.toggleSearch(); h.submitQuery("猫"); h.toggleBatch(); h.toggleSelected("BV1")
            h.batchAction("copy",0,LibraryUiState()); h.pageChanged()
            assertFalse(h.state.value.batchMode); assertTrue(h.state.value.selected.isEmpty())
            assertTrue(h.state.value.searching); assertEquals("猫",h.state.value.query); assertEquals(2,h.state.value.transferMode)
        }
    }
    @Test fun searchCloseAndBackKeepTheirDifferentOriginalSelectionRules() {
        Fixture().use { f ->
            val h=f.holder; h.toggleBatch(); h.toggleSelected("BV1"); h.toggleSearch(); h.submitQuery("猫")
            h.closeSearch(); assertEquals("",h.state.value.query)
            assertTrue(h.state.value.batchMode); assertEquals(setOf("BV1"),h.state.value.selected)
            h.toggleSearch(); h.submitQuery("猫"); h.toggleSearch()
            assertFalse(h.state.value.searching); assertEquals("",h.state.value.query)
        }
    }
    @Test fun submitTrimsOnlyCommittedQueryAndHistoryUpdatesFromOtherPages() {
        Fixture().use { f ->
            val h=f.holder; h.submitQuery("  CAT  "); assertEquals("CAT",h.state.value.query)
            assertEquals(listOf("CAT"),h.state.value.searchHistory)
            h.submitQuery("   "); assertEquals("CAT",h.state.value.query)
            // 页面级搜索记录：别的页面/首页写入不会串进本页。
            f.settings.addSearchHistory("library_1", "external")
            assertEquals("CAT",h.state.value.searchHistory.first())
            assertTrue(f.settings.searchHistory.isEmpty())
            f.settings.addSearchHistory("library_0", "own")
            assertEquals("own",h.state.value.searchHistory.first())
            h.clearSearchHistory(); assertEquals("",h.state.value.query); assertTrue(h.state.value.searchHistory.isEmpty())
            assertTrue(f.settings.searchHistory.isEmpty())
        }
    }
    @Test fun pageSearchHistoryIsIsolatedPerLibraryPage() {
        Fixture().use { f ->
            val favorites=f.holder
            val history=LibraryInteractionStateHolder(f.settings.general, f.settings.state.map { Unit }, f.scope, "library_1")
            favorites.submitQuery("猫")
            assertTrue(history.state.value.searchHistory.isEmpty())
            assertTrue(f.settings.searchHistory.isEmpty())
            history.submitQuery("狗")
            assertEquals(listOf("猫"),favorites.state.value.searchHistory)
            assertEquals(listOf("狗"),history.state.value.searchHistory)
            history.close()
        }
    }
    @Test fun filtersMatchTitlesAndAuthorsIgnoringCaseWithoutChangingOrder() {
        Fixture().use { f ->
            val h=f.holder; h.submitQuery("CAT")
            assertEquals(listOf("BV1","BV2"),h.visibleVideos(listOf(video(1,"Cat"),video(2,"other","cat"),video(3,"dog"))).map { it.bvid })
            assertEquals(listOf("BV2","BV1"),h.visibleEntries(listOf(entry(2,"other","cat"),entry(3,"dog"),entry(1,"Cat"))).map { it.bvid })
        }
    }
    @Test fun watchLaterAllSelectionIncludesFilteredItemsBeyondTwentyVisibleCards() {
        Fixture().use { f ->
            val h=f.holder; val data=LibraryUiState(watchLater=(1..60).map { entry(it) })
            assertEquals(20,h.state.value.watchLaterVisibleCount)
            h.batchAction("all",2,data); assertEquals(60,h.state.value.selected.size)
            h.batchAction("all",2,data); assertTrue(h.state.value.selected.isEmpty())
            h.toggleSelected("stale"); h.batchAction("all",2,data); assertEquals(60,h.state.value.selected.size)
        }
    }
    @Test fun selectAllRetainsCountBasedToggleEvenWhenIdsDiffer() {
        Fixture().use { f ->
            val h=f.holder; h.toggleSelected("other")
            h.batchAction("all",0,LibraryUiState(favorites=FavoriteUiState(videos=listOf(video(1)))))
            assertTrue(h.state.value.selected.isEmpty())
        }
    }
    @Test fun watchLaterCallbacksReadCurrentCountAndFullListSizeAndResetToTwenty() {
        Fixture().use { f ->
            val h=f.holder; var currentSize=0
            val loadMore={ h.showMoreWatchLater(currentSize) }
            currentSize=55; loadMore(); assertEquals(40,h.state.value.watchLaterVisibleCount)
            loadMore(); assertEquals(55,h.state.value.watchLaterVisibleCount)
            h.resetWatchLaterCount(); assertEquals(20,h.state.value.watchLaterVisibleCount)
        }
    }
    @Test fun deleteAndTransferCaptureSelectionBeforeClearingUiAndRespectMode() {
        Fixture().use { f ->
            val h=f.holder; h.toggleBatch(); h.toggleSelected("BV1")
            assertNull(h.batchAction("move",1,LibraryUiState())); assertNull(h.state.value.transferMode)
            h.batchAction("copy",0,LibraryUiState()); val transfer=h.beginTransfer(2)!!
            assertFalse(transfer.moving); assertEquals(setOf("BV1"),transfer.ids)
            assertNull(h.state.value.transferMode); assertFalse(h.state.value.batchMode); assertTrue(h.state.value.selected.isEmpty())
            h.toggleBatch(); h.toggleSelected("BV2"); val delete=h.batchAction("delete",1,LibraryUiState())!!
            assertEquals(LibraryDeleteSelection(1,setOf("BV2")),delete)
            assertFalse(h.state.value.batchMode); assertTrue(h.state.value.selected.isEmpty())
        }
    }
    @Test fun closedInteractionRejectsLateUiAndHistoryWrites() {
        Fixture().use { f ->
            val h=f.holder; h.close(); val before=h.state.value
            h.toggleSearch(); h.toggleBatch(); h.toggleSelected("BV1"); h.pageChanged(); h.selectFolder()
            h.submitQuery("late"); h.clearSearchHistory(); h.showMoreWatchLater(60); h.resetWatchLaterCount()
            h.closeSearch(); h.dismissTransfer()
            assertNull(h.batchAction("delete",0,LibraryUiState())); assertNull(h.beginTransfer(1)); assertFalse(h.back())
            f.settings.addSearchHistory("new owner")
            assertEquals(before,h.state.value); assertEquals(listOf("new owner"),f.settings.searchHistory)
        }
    }
}
