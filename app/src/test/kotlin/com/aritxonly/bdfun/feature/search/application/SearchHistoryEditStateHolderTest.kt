package com.aritxonly.bdfun.feature.search.application

import com.aritxonly.bdfun.feature.settings.application.MemorySettingsPreferences
import com.aritxonly.bdfun.feature.settings.application.SettingsStateHolder
import com.aritxonly.bdfun.feature.settings.application.TestHistoryCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryEditStateHolderTest {

    /** 首页搜索记录的落盘实现（settings.general），用内存 preferences 替身。 */
    private class Fixture : AutoCloseable {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val settings = SettingsStateHolder.create(MemorySettingsPreferences(), TestHistoryCodec, scope)
        override fun close() { scope.cancel() }
    }

    private fun holder(history: List<String>) = SearchHistoryEditStateHolder().also { it.begin(history) }

    @Test fun beginSnapshotsVisibleAndStartsWithoutRemoval() {
        val h = holder(listOf("A", "B", "C"))
        assertTrue(h.state.value.editing)
        assertEquals(listOf("A", "B", "C"), h.state.value.visible)
        assertTrue(h.state.value.removed.isEmpty())
    }

    @Test fun beginIsIgnoredWhileAlreadyEditingSoProgressIsKept() {
        val h = holder(listOf("A", "B"))
        h.remove("A")
        h.begin(listOf("A", "B"))
        assertEquals(listOf("B"), h.state.value.visible)
        assertEquals(listOf("A"), h.state.value.removed)
    }

    @Test fun removeOnlyAffectsDraftAndIgnoresUnknownWords() {
        val h = holder(listOf("A", "B"))
        h.remove("A")
        h.remove("A")
        h.remove("不存在")
        assertEquals(listOf("B"), h.state.value.visible)
        assertEquals(listOf("A"), h.state.value.removed)
    }

    @Test fun cancelDiscardsDraftAndNeverApplies() {
        val h = holder(listOf("A", "B"))
        h.remove("A")
        h.cancel()
        assertFalse(h.state.value.editing)
        assertTrue(h.state.value.visible.isEmpty())
        assertTrue(h.state.value.removed.isEmpty())
        var applied: Collection<String>? = null
        h.commit { applied = it }
        assertEquals(null, applied)
    }

    @Test fun commitAppliesRemovedWordsOnceAndLeavesEditMode() {
        val h = holder(listOf("A", "B", "C"))
        h.remove("B")
        val calls = mutableListOf<Collection<String>>()
        val removed = h.commit { calls += it }
        assertEquals(listOf("B"), removed)
        assertEquals(listOf(listOf("B")), calls)
        assertFalse(h.state.value.editing)
        // 已退出编辑态，再点保存不能重复删一遍。
        h.commit { calls += it }
        assertEquals(1, calls.size)
    }

    @Test fun commitWithoutRemovalsDoesNotTouchStorage() {
        val h = holder(listOf("A"))
        var applied = false
        h.commit { applied = true }
        assertFalse(applied)
        assertFalse(h.state.value.editing)
    }

    @Test fun removingEveryEntryThenCommittingClearsStorageOnRealSettings() {
        Fixture().use { f ->
            listOf("猫", "狗").forEach { f.settings.addSearchHistory(it) }
            val h = holder(f.settings.searchHistory)
            f.settings.searchHistory.forEach(h::remove)
            h.commit { removed -> f.settings.removeSearchHistory(removed) }
            assertTrue(f.settings.searchHistory.isEmpty())
        }
    }

    @Test fun removalKeepsOrderAndOnlyDeletesTouchedWords() {
        Fixture().use { f ->
            listOf("一", "二", "三").forEach { f.settings.addSearchHistory(it) }
            // addSearchHistory 是新的在前，落盘顺序为 三 二 一。
            assertEquals(listOf("三", "二", "一"), f.settings.searchHistory)
            f.settings.removeSearchHistory(listOf("二"))
            assertEquals(listOf("三", "一"), f.settings.searchHistory)
            f.settings.removeSearchHistory(emptyList())
            assertEquals(listOf("三", "一"), f.settings.searchHistory)
        }
    }

    @Test fun removalDoesNotTouchScopedLibraryHistory() {
        Fixture().use { f ->
            f.settings.addSearchHistory("首页")
            f.settings.addSearchHistory("library_0", "收藏页")
            f.settings.removeSearchHistory(listOf("首页"))
            assertTrue(f.settings.searchHistory.isEmpty())
            assertEquals(listOf("收藏页"), f.settings.searchHistory("library_0"))
        }
    }
}