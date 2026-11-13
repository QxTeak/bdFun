package com.aritxonly.bdfun

import com.aritxonly.bdfun.navigateToWatchLater
import com.aritxonly.bdfun.navigateToVideo
import com.aritxonly.bdfun.navigateToSpace
import com.aritxonly.bdfun.navigateToSettings
import com.aritxonly.bdfun.navigateToMySent
import com.aritxonly.bdfun.navigateToLogin
import com.aritxonly.bdfun.navigateToLibrary
import com.aritxonly.bdfun.navigateToHistory
import com.aritxonly.bdfun.navigateToDynamic
import com.aritxonly.bdfun.navigateToCache
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalViewConfiguration
import com.aritxonly.bdfun.ui.rememberStricterSwipeConfiguration
import com.aritxonly.bdfun.core.model.user.UserProfile
import com.aritxonly.bdfun.feature.settings.presentation.SettingsBindings
import com.aritxonly.bdfun.ui.DynamicScreen
import com.aritxonly.bdfun.ui.MineScreen

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun MainTabPager(
    pager: androidx.compose.foundation.pager.PagerState,
    homeFeed: com.aritxonly.bdfun.feature.home.presentation.HomeFeedState,
    dynamicFeed: com.aritxonly.bdfun.feature.dynamic.presentation.DynamicFeedState,
    session: UserProfile?, settings: SettingsBindings, dynamicRefreshTick: Int,
    mineList: androidx.compose.foundation.lazy.LazyListState,
    homeSection: Int = 0,
    onPhotoOpenChange: (Boolean) -> Unit = {},
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    CompositionLocalProvider(
        LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
    ) {
        HorizontalPager(
            state = pager,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 0,
            // 到头/到尾再滑不出现任何回弹动画
            overscrollEffect = null,
        ) { page ->
            when (page) {
                0 -> if (homeSection == 0) {
                    HomeScreenRoute(
                        state = homeFeed,
                        onOpenVideo = { navigateToVideo(context, it) },
                        cardStyle = settings.homeCardStyle,
                    )
                } else {
                    // 直播 / 番剧：页面还没做，先留空。顶栏 tab 可以切换到这里，
                    // 不显示任何占位文案，避免以后接入真实页面时还要删一遍。
                    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize())
                }

                1 -> DynamicScreen(
                    state = dynamicFeed,
                    loggedIn = session != null,
                    onNeedLogin = { navigateToLogin(context) },
                    onOpenVideo = { navigateToVideo(context, it) },
                    onOpenDynamic = { navigateToDynamic(context, it) },
                    refreshTick = dynamicRefreshTick,
                    onPhotoOpenChange = onPhotoOpenChange,
                )

                else -> MainMineTab(session, settings, mineList)
            }
        }
    }
}

@Composable
private fun MainMineTab(session: UserProfile?, settings: SettingsBindings, mineList: androidx.compose.foundation.lazy.LazyListState) {
    val context = androidx.compose.ui.platform.LocalContext.current
    MineScreen(
        com.aritxonly.bdfun.ui.MineScreenRequest(session, settings.sentHistoryEntry, AppGraph.session.identity.revision),
        com.aritxonly.bdfun.ui.MineNavigation(
            { navigateToLogin(context) }, { navigateToSettings(context) },
            { navigateToLibrary(context) }, { navigateToHistory(context) },
            { navigateToWatchLater(context) }, { navigateToCache(context) },
            { navigateToMySent(context) }, { navigateToSpace(context, it) },
            { mid, fans -> RelationActivity.open(context, mid, fans) }, { navigateToVideo(context, it) },
        ),
        AppGraph.mineDependencies, mineList,
    )
}
