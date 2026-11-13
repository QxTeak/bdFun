package com.aritxonly.bdfun.feature.space.presentation

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aritxonly.bdfun.core.application.SessionIdentity
import com.aritxonly.bdfun.core.application.SessionResourceSlot
import com.aritxonly.bdfun.core.common.Outcome
import com.aritxonly.bdfun.core.domain.SpaceRepository
import com.aritxonly.bdfun.core.model.feed.FeedPage
import com.aritxonly.bdfun.core.model.user.*
import com.aritxonly.bdfun.core.model.video.VideoItem
import com.aritxonly.bdfun.feature.space.application.*
import org.junit.Assert.*
import org.junit.Test

class SpacePageViewModelTest {
    private val unused = object : SpaceRepository {
        override suspend fun profile(mid: Long): Outcome<SpaceInfo> = error("unused")
        override suspend fun seasons(mid: Long): Outcome<List<SpaceSeason>> = error("unused")
        override suspend fun dynamics(mid: Long, offset: String): Outcome<FeedPage> = error("unused")
        override suspend fun relation(mid: Long): Outcome<Int?> = error("unused")
        override suspend fun videos(mid: Long, page: Int): Outcome<List<VideoItem>> = error("unused")
        override suspend fun articles(mid: Long, page: Int): Outcome<List<SpaceArticle>> = error("unused")
        override suspend fun bangumi(mid: Long, page: Int): Outcome<List<SpaceBangumi>> = error("unused")
        override suspend fun shop(mid: Long): Outcome<List<SpaceShopItem>> = error("unused")
        override suspend fun collection(mid: Long, collection: SpaceSeason): Outcome<List<VideoItem>> = error("unused")
        override suspend fun follow(mid: Long, followed: Boolean): Outcome<Boolean> = error("unused")
    }
    private var identity = SessionIdentity(1, 1)
    private var factories = 0
    private var released = 0
    private fun provider(store: ViewModelStore) = ViewModelProvider(store, viewModelFactory {
        initializer {
            factories++
            SpacePageViewModel(SessionResourceSlot({ identity }, {
                val owner = identity
                SpaceStateHolder(1, unused) { owner == identity }
            }, { it.close(); released++ }))
        }
    })

    @Test fun retainedStoreReusesPageAcrossProviderRecreation() {
        val store = ViewModelStore()
        try {
            val first = provider(store)[SpacePageViewModel::class.java]; val state = first.currentState()
            val recreated = provider(store)[SpacePageViewModel::class.java]
            assertSame(first, recreated); assertSame(state, recreated.currentState())
            assertEquals(1, factories); assertEquals(0, released)
        } finally { store.clear() }
        assertEquals(1, released)
    }
    @Test fun twoPagesForSameSpaceNeverShareMutableControllerOrReleaseEachOther() {
        val a = ViewModelStore(); val b = ViewModelStore()
        try {
            val first = provider(a)[SpacePageViewModel::class.java].currentState()
            val second = provider(b)[SpacePageViewModel::class.java].currentState()
            assertNotSame(first, second)
            a.clear(); assertNull(first.nextPage(2, 0))
            assertEquals(SpacePageRequest.Videos(2), second.nextPage(2, 0))
            assertEquals(1, released)
        } finally { a.clear(); b.clear() }
        assertEquals(2, released)
    }
    @Test fun accountRoundTripReplacesControllerWithinSameRetainedPage() {
        val store = ViewModelStore()
        try {
            val page = provider(store)[SpacePageViewModel::class.java]; val old = page.currentState()
            identity = SessionIdentity(2, 2); identity = SessionIdentity(1, 3)
            val current = page.currentState()
            assertNotSame(old, current); assertNull(old.nextPage(2, 0))
            assertEquals(SpacePageRequest.Videos(2), current.nextPage(2, 0)); assertEquals(1, released)
        } finally { store.clear() }
        assertEquals(2, released)
    }
    @Test fun storeClearClosesPageExactlyOnceAndRejectsReuse() {
        val store = ViewModelStore(); val page = provider(store)[SpacePageViewModel::class.java]
        val old = page.currentState(); store.clear(); store.clear()
        assertEquals(1, released); assertNull(old.nextPage(1, 0))
        assertThrows(IllegalStateException::class.java) { page.currentState() }
    }
}
