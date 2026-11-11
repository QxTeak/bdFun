package com.aritxonly.bdfun.feature.library.application

import org.junit.Assert.*
import org.junit.Test

class FavoriteFolderOperationsTest {
    @Test fun rapidClicksCannotSubmitTheSameStaleSelectionTwice() {
        val operations = FavoriteFolderOperations()
        assertTrue(operations.tryStart(1L))
        assertFalse(operations.tryStart(1L))
        assertFalse(operations.tryStart(2L))
        assertEquals(1L, operations.pending.value)
        operations.finish(1L)
        assertTrue(operations.tryStart(2L))
    }

    @Test fun staleCompletionCannotUnlockANewerRequest() {
        val operations = FavoriteFolderOperations()
        assertTrue(operations.tryStart(1L))
        operations.finish(1L)
        assertTrue(operations.tryStart(2L))
        operations.finish(1L)
        assertEquals(2L, operations.pending.value)
        operations.finish(2L)
        assertNull(operations.pending.value)
    }
}
