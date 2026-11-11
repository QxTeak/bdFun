package com.aritxonly.bdfun

import androidx.lifecycle.repeatOnLifecycle
import com.aritxonly.teak.common.chrome.LocalTeakPageScrollState
import com.aritxonly.teak.common.chrome.LocalTopBarScrollProgress
import com.aritxonly.teak.common.chrome.ProgressiveTopBarMaterial
import com.aritxonly.teak.common.chrome.TrackTeakPageScroll
import com.aritxonly.teak.common.chrome.isWideFoldTopBar
import com.aritxonly.teak.common.chrome.rememberTeakPageScrollState
import com.aritxonly.teak.common.chrome.TeakChromeDefaults
import com.aritxonly.teak.common.chrome.TeakGlassBackButton
import com.aritxonly.teak.common.chrome.TeakPageChrome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.DisposableEffect
import com.aritxonly.bdfun.ui.ScrollTopButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.compose.foundation.layout.Row
import top.yukonga.miuix.kmp.icon.extended.Search
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.aritxonly.teak.common.navigation.TeakFloatingTabBar
import com.aritxonly.teak.common.navigation.TeakFloatingTabBarDefaults
import com.aritxonly.teak.common.navigation.TeakFloatingTabItem
import com.aritxonly.teak.common.navigation.TeakFloatingTabLayout
import com.aritxonly.teak.common.theme.TeakUiStyle
import com.aritxonly.bdfun.ui.rememberStricterSwipeConfiguration
import com.aritxonly.teak.common.material.LocalTeakBackdrop
import com.aritxonly.teak.common.theme.TeakTheme
import com.aritxonly.bdfun.data.BiliApi
import com.aritxonly.bdfun.data.SettingsStore
import com.aritxonly.bdfun.data.UserProfile
import com.aritxonly.bdfun.data.VideoDetail
import com.aritxonly.bdfun.data.VideoItem
import com.aritxonly.bdfun.ui.CommentScreen
import com.aritxonly.bdfun.ui.findHostActivity
import com.aritxonly.bdfun.ui.syncSystemBarIcons
import com.aritxonly.bdfun.ui.DynamicScreen
import com.aritxonly.bdfun.ui.DynamicTopBar
import com.aritxonly.bdfun.ui.HomePullIndicator
import com.aritxonly.bdfun.ui.HomeScreen
import com.aritxonly.bdfun.ui.HomeTopBar
import com.aritxonly.bdfun.ui.HomeTopBarHeight
import com.aritxonly.bdfun.ui.LibraryScreen
import com.aritxonly.bdfun.ui.LoginScreen
import com.aritxonly.bdfun.ui.MessageScreen
import com.aritxonly.bdfun.ui.MineScreen
import com.aritxonly.bdfun.ui.MySentScreen
import com.aritxonly.bdfun.ui.SearchOverlay
import com.aritxonly.bdfun.ui.SettingsScreen
import com.aritxonly.bdfun.ui.TabTopBar
import com.aritxonly.bdfun.ui.TeakSplash
import com.aritxonly.bdfun.ui.UiSwapDurationMs
import com.aritxonly.bdfun.ui.VideoScreen
import com.aritxonly.bdfun.ui.blockTouches
import com.aritxonly.bdfun.ui.rememberDynamicFeedState
import com.aritxonly.bdfun.ui.rememberHomeFeedState
import com.aritxonly.bdfun.ui.rememberManualProgress
import com.aritxonly.bdfun.ui.rememberSpringFloat
import com.aritxonly.bdfun.ui.BackgroundPlayback
import com.aritxonly.bdfun.ui.BackgroundPlayDisc
import com.aritxonly.bdfun.ui.rememberDiscPlacement
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ContactsCircle
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Messages
import top.yukonga.miuix.kmp.theme.darkColorScheme as miuixDarkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme as miuixLightColorScheme

/** HyperOS 色板：B 站主色 + Miuix 表面层级。 */
internal object ThemePalette {
    private val AcPink = Color(0xFFFB7299)
    private val AcPinkDeep = Color(0xFFE45C82)
    /** Miuix 默认蓝（Hyper 蓝）。 */
    private val HyperBlue = Color(0xFF3482FF)
    private val HyperBlueDeep = Color(0xFF1D63D8)

    /** 0 = AC 粉，1 = Hyper 蓝。 */
    var style by mutableStateOf(0)

    val brand: Color get() = if (style == 1) HyperBlue else AcPink
    val brandDeep: Color get() = if (style == 1) HyperBlueDeep else AcPinkDeep
}

internal val BrandPink: Color get() = ThemePalette.brand
internal val BrandPinkDeep: Color get() = ThemePalette.brandDeep

private data class Destination(
    val key: String,
    val label: String,
    val icon: ImageVector,
)

internal fun hyperLightColors() = miuixLightColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPink,
    onPrimaryVariant = Color(0xFF5A2735),
    secondaryVariant = BrandPink,
    onSecondaryVariant = Color.White,
    // 参考截图：页面底色浅灰 #F3F3F3，圆角卡片纯白 #FFFFFF
    background = Color(0xFFF3F3F3),
    onBackground = Color(0xFF16181D),
    onBackgroundVariant = Color(0xFF6B7280),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF16181D),
    surfaceVariant = Color(0xFFF7F7F7),
    surfaceContainer = Color(0xFFF7F7F7),
    onSurfaceContainer = Color(0xFF16181D),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    onSurfaceContainerHigh = Color(0xFF16181D),
    surfaceContainerHighest = Color(0xFFE9E9E9),
    onSurfaceContainerHighest = Color(0xFF16181D),
    outline = Color(0xFFE6E6E6),
    dividerLine = Color(0x14000000),
)

internal fun hyperDarkColors() = miuixDarkColorScheme(
    primary = BrandPink,
    onPrimary = Color.White,
    primaryVariant = BrandPinkDeep,
    onPrimaryVariant = Color(0xFFFFD9E3),
    secondaryVariant = BrandPinkDeep,
    onSecondaryVariant = Color.White,
    background = Color(0xFF0D0E11),
    onBackground = Color(0xFFF2F3F5),
    onBackgroundVariant = Color(0xFF9AA0AA),
    surface = Color(0xFF121317),
    onSurface = Color(0xFFF2F3F5),
    surfaceVariant = Color(0xFF1B1D22),
    surfaceContainer = Color(0xFF1A1C21),
    onSurfaceContainer = Color(0xFFF2F3F5),
    surfaceContainerHigh = Color(0xFF24272E),
    onSurfaceContainerHigh = Color(0xFFF2F3F5),
    surfaceContainerHighest = Color(0xFF2D3038),
    onSurfaceContainerHighest = Color(0xFFF2F3F5),
    outline = Color(0xFF2B2E35),
    dividerLine = Color(0x1AFFFFFF),
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun BdFunApp(api: BiliApi, settings: SettingsStore) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.darkMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val light = remember(settings.colorStyle) { hyperLightColors() }
    val darkColors = remember(settings.colorStyle) { hyperDarkColors() }
    val materialColors = if (dark) {
        darkColorScheme(
            primary = BrandPink,
            background = Color(0xFF0D0E11),
            surface = Color(0xFF121317),
            surfaceVariant = Color(0xFF1A1C21),
            onSurface = Color(0xFFF2F3F5),
            outline = Color(0xFF2B2E35),
        )
    } else {
        lightColorScheme(
            primary = BrandPink,
            background = Color(0xFFF3F3F3),
            surface = Color(0xFFFFFFFF),
            surfaceVariant = Color(0xFFF7F7F7),
            onSurface = Color(0xFF16181D),
            outline = Color(0xFFE6E6E6),
        )
    }

    val view = LocalView.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    SideEffect {
        // 走 ContextWrapper 链找 Activity：直接强转在部分机型上会拿不到，图标就不跟着主题变
        val activity = view.context.findHostActivity()
        activity ?: return@SideEffect
        activity.syncSystemBarIcons(dark)
    }

    val destinations = remember {
        listOf(
            Destination("home", "首页", MiuixIcons.Home),
            Destination("dynamics", "动态", MiuixIcons.Album),
            Destination("mine", "我的", MiuixIcons.ContactsCircle),
        )
    }
    // 三个 tab 用 HorizontalPager：页面之间横向相连，手拖和切换都是同一套非线性动画
    val tabPagerState = rememberPagerState(pageCount = { destinations.size })
    val selected = tabPagerState.currentPage
    val homeChrome = rememberTeakPageScrollState()
    val dynamicChrome = rememberTeakPageScrollState()
    val searchChrome = rememberTeakPageScrollState()
    // 登录态放在进程级单例，LoginActivity 登录成功后直接写，这里立即刷新
    var session by AppGraph.session
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var searchSubmitted by remember { mutableStateOf<String?>(null) }
    // 搜索页渐变进度：和头像/消息那两个插槽的切换速率完全一致
    val searchReveal = rememberManualProgress(if (showSearch) 1f else 0f, UiSwapDurationMs)

    val closeSearch: () -> Unit = {
        showSearch = false
        searchQuery = ""
        searchSubmitted = null
    }
    // 开屏只在本进程首次出现；配置变化（旋转）不重播。
    var splashVisible by rememberSaveable { mutableStateOf(true) }
    // 再次点击当前底栏项 → 触发该页刷新
    var dynamicRefreshTick by remember { mutableIntStateOf(0) }
    val appScope = rememberCoroutineScope()
    var unreadMessageCount by remember(session?.mid) { mutableIntStateOf(0) }
    val unreadLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    LaunchedEffect(session?.mid, unreadLifecycleOwner) {
        if (session == null) return@LaunchedEffect
        unreadLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
            while (true) {
                try { unreadMessageCount = api.messageUnreadCount() }
                catch (cancel: kotlinx.coroutines.CancellationException) { throw cancel }
                catch (_: Exception) { }
                kotlinx.coroutines.delay(60_000)
            }
        }
    }


    LaunchedEffect(Unit) {
        session = runCatching { api.nav() }.getOrNull()
    }
    // 首页数据放在 App 层：进视频/搜索/设置再返回时不会重新拉取，滚动位置也保留。
    val homeFeed = rememberHomeFeedState(api)
    val dynamicFeed = rememberDynamicFeedState(api, session?.mid)
    // 首页 / 动态页各自注册回到顶部；其他底栏页不显示。
    when (selected) {
        0 -> com.aritxonly.bdfun.ui.RegisterScrollTop(homeFeed.gridState, "home")
        1 -> com.aritxonly.bdfun.ui.RegisterScrollTop(dynamicFeed.listState, "dynamic")
    }
    TrackTeakPageScroll(homeFeed.gridState, homeChrome)
    TrackTeakPageScroll(dynamicFeed.listState, dynamicChrome)
    // 参考 HyperIsland 的做法：整屏一块 backdrop，录制时先铺一层 surface 再画内容
    val backdrop = rememberLayerBackdrop { drawRect(materialColors.background); drawContent() }
    // 搜索页自己一块：首页顶栏的渐变模糊和筛选菜单都采样它，复用同一套页面结构。
    val searchBackdrop = rememberLayerBackdrop {
        drawRect(materialColors.background)
        drawContent()
    }
    val tabItems = destinations.map { destination ->
        TeakFloatingTabItem(
            key = destination.key,
            label = destination.label,
            selectedIcon = rememberVectorPainter(destination.icon),
            unselectedIcon = rememberVectorPainter(destination.icon),
            iconScale = if (destination.key == "home") 0.95f else 1f,
        )
    }

    // 界面风格是全局对象：设置页改完后这里同步一次，所有 TeakUI 组件立即响应。
    SideEffect { TeakUiStyle.style = settings.interfaceStyle }

    val wideNavigation = isWideFoldTopBar() && androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp >= 600

    TeakTheme(
        dark = dark,
        lightColors = light,
        darkColors = darkColors,
        materialColors = materialColors,
        blurEnabled = settings.blurEnabled,
        refractionEnabled = settings.refractionEnabled,
        fontScale = settings.fontScale,
    ) {
        CompositionLocalProvider(LocalTeakBackdrop provides backdrop) {
                // 首页在开屏下面先组合好（网络同时开跑），开屏淡出即交叉淡化进入首页，不白屏。
                // 系统返回键：先关搜索 / 覆盖页，再退出 App
                androidx.activity.compose.BackHandler(enabled = showSearch) {
                    if (searchSubmitted != null) {
                        searchSubmitted = null
                    } else {
                        closeSearch()
                    }
                }
                // 后台播放时，从首页按返回不结束 App：整个任务收到后台，声音继续。
                androidx.activity.compose.BackHandler(
                    enabled = !showSearch &&
                        settings.backgroundPlay &&
                        BackgroundPlayback.active,
                ) {
                    context.findHostActivity()?.moveTaskToBack(true)
                }

                // 当前页面标识：用于页面过渡动画 + 返回键状态复位
                val pageKey = if (showSearch) "search" else "tab$selected"
                LaunchedEffect(pageKey) { TeakPageChrome.reset() }


                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(materialColors.background),
                ) {
                    // 底栏玻璃的采样源：这一层就是整个页面内容。
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layerBackdrop(backdrop),
                    ) {
                        // 三个 tab：横向相连的 pager（可拖拽，切换是同一套非线性动画）
                        // 外面包一层更大的触摸阈值，手指轻抖不会误翻页。
                        CompositionLocalProvider(
                            LocalViewConfiguration provides rememberStricterSwipeConfiguration(),
                        ) {
                        HorizontalPager(
                            state = tabPagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 0,
                            // 到头/到尾再滑不出现任何回弹动画
                            overscrollEffect = null,
                        ) { page ->
                            when (page) {
                                0 -> HomeScreen(
                                    state = homeFeed,
                                    onOpenVideo = { TeakNavigator.openVideo(context, it) },
                                    cardStyle = settings.homeCardStyle,
                                )

                                1 -> DynamicScreen(
                                    state = dynamicFeed,
                                    loggedIn = session != null,
                                    onNeedLogin = { TeakNavigator.openLogin(context) },
                                    onOpenVideo = { TeakNavigator.openVideo(context, it) },
                                    onOpenDynamic = { TeakNavigator.openDynamic(context, it) },
                                    refreshTick = dynamicRefreshTick,
                                )

                                else -> MineScreen(
                                    profile = session,
                                    onLogin = { TeakNavigator.openLogin(context) },
                                    onOpenSettings = { TeakNavigator.openSettings(context) },
                                    onOpenLibrary = { TeakNavigator.openLibrary(context) },
                                    onOpenHistory = { TeakNavigator.openHistory(context) },
                                    onOpenWatchLater = { TeakNavigator.openWatchLater(context) },
                                    onOpenCache = { TeakNavigator.openCache(context) },
                                    onOpenMessages = { TeakNavigator.openMessages(context) },
                                    onLogout = {
                                        api.logout()
                                        session = null
                                    },
                                    showSentHistory = settings.sentHistoryEntry,
                                    onOpenSentHistory = { TeakNavigator.openMySent(context) },
                                )
                            }
                        }
                        }

                    }

                    val pageFloat = (tabPagerState.currentPage + tabPagerState.currentPageOffsetFraction)
                        .coerceAtLeast(0f)
                    ScrollTopButton(
                        settings,
                        bottomObstruction = if (showSearch) 0.dp else
                            TeakFloatingTabBarDefaults.Height + when {
                                wideNavigation -> 12.dp
                                settings.bottomBarRaise -> 4.dp + TeakChromeDefaults.RaisedBottomBarExtra
                                else -> 4.dp
                            },
                    )

                    // 搜索页：搜索框保持不动，下面整块换成纯白页（搜索记录 + 热搜 / 结果）
                    if (showSearch || searchReveal > 0.01f) {
                        // 搜索页复刻首页结构：内容先录进 searchBackdrop，
                        // 顶栏的渐变模糊和筛选菜单都采样同一块。
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .layerBackdrop(searchBackdrop),
                        ) {
                            CompositionLocalProvider(
                                LocalTeakBackdrop provides searchBackdrop,
                                LocalTeakPageScrollState provides searchChrome,
                            ) {
                                SearchOverlay(
                                    api = api,
                                    settings = settings,
                                    submitted = searchSubmitted,
                                    reveal = searchReveal,
                                    query = searchQuery,
                                    onPickKeyword = { word ->
                                        searchQuery = word
                                        settings.addSearchHistory(word)
                                        searchSubmitted = word
                                        keyboard?.hide()
                                    },
                                    onOpenVideo = { TeakNavigator.openVideo(context, it) },
                                    onOpenUser = { TeakNavigator.openSpace(context, it.mid) },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                        }
                    }
                    // 首页顶栏：挂在采集层之外，所以能直接用底栏同款玻璃（真模糊）+ 渐变式模糊
                    // 顶栏跟首页一起走：位置随 pager 偏移平移，透明度同步降低
                    if (pageFloat < 0.99f) {
                        // 搜索态顶栏改采样搜索页自己的 backdrop：
                        // 结果列表从下面滚过时，顶部就是首页同款渐进式模糊。
                        CompositionLocalProvider(
                            LocalTeakBackdrop provides if (showSearch) {
                                searchBackdrop
                            } else {
                                LocalTeakBackdrop.current
                            },
                        ) {
                        CompositionLocalProvider(LocalTopBarScrollProgress provides (if (showSearch) searchChrome.materialProgress else homeChrome.materialProgress)) {
                        ProgressiveTopBarMaterial(
                            modifier = Modifier.align(Alignment.TopCenter)
                                .graphicsLayer {
                                    translationX = -pageFloat * size.width * 0.92f
                                    alpha = (1f - pageFloat).coerceIn(0f, 1f)
                                },
                        ) {
                        HomeTopBar(
                            profile = session,
                            unreadCount = unreadMessageCount,
                            searchActive = showSearch,
                            searchQuery = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onSubmitSearch = {
                                val word = searchQuery.trim()
                                if (word.isNotEmpty()) {
                                    settings.addSearchHistory(word)
                                    searchSubmitted = word
                                    keyboard?.hide()
                                }
                            },
                            onCancelSearch = {
                                if (searchSubmitted != null) searchSubmitted = null else closeSearch()
                            },
                            onClearQuery = { searchQuery = "" },
                            searchReveal = searchReveal,
                            onSearch = {
                                searchChrome.reset()
                                showSearch = true
                                searchQuery = ""
                                searchSubmitted = null
                            },
                            searchResultsVisible = showSearch && searchSubmitted != null,
                            onOpenSearchPage = {
                                // 结果页再点搜索框：回到搜索首页（历史/联想），不要直接改当前结果。
                                searchSubmitted = null
                                searchQuery = ""
                                keyboard?.show()
                            },
                            onOpenMessages = { TeakNavigator.openMessages(context) },
                            onOpenProfile = {
                                if (session == null) TeakNavigator.openLogin(context) else appScope.launch { tabPagerState.animateScrollToPage(2) }
                            },
                            modifier = Modifier,
                        )
                        }
                        }
                        }

                        // 使用须知改由独立的 TermsActivity 在启动时展示。

                    // 下拉刷新指示器：独立覆盖层，挂在 App 层，探出顶栏也不会被裁掉。
                        // 透明度直接由下拉位移决定：位移 58dp 开始淡入、84dp 全显；
                        // 卡片在位移 <46dp 时才会撞到小电视，因此永远追不上。
                        HomePullIndicator(
                            reveal = if (homeFeed.refreshing) {
                                1f
                            } else {
                                ((homeFeed.pullOffsetDp - 58f) / 26f).coerceIn(0f, 1f)
                            },
                            animated = homeFeed.refreshing,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .statusBarsPadding()
                                .padding(top = HomeTopBarHeight + 4.dp),
                        )
                    }

                    // 动态页固定顶栏：只有「全部 / 视频」玻璃分段栏。
                    // 头像卡片已经放进动态列表里，不再固定在顶栏。
                    if (pageFloat > 0.01f && pageFloat < 1.99f &&
                        session != null && !showSearch
                    ) {
                        CompositionLocalProvider(LocalTopBarScrollProgress provides dynamicChrome.materialProgress) {
                        ProgressiveTopBarMaterial(
                            modifier = Modifier.align(Alignment.TopCenter)
                                .graphicsLayer {
                                    translationX = (1f - pageFloat) * size.width * 0.92f
                                    alpha = (1f - kotlin.math.abs(pageFloat - 1f)).coerceIn(0f, 1f)
                                },
                        ) {
                        DynamicTopBar(
                            state = dynamicFeed,
                            loggedIn = session != null,
                            modifier = Modifier,
                        )
                    }
                    }
                    }

                    // 视频详情页隐藏底栏，避免遮挡播放器控件
                    val tabBarCovered = showSearch
                    val tabBarAlpha = rememberManualProgress(
                        if (tabBarCovered) 0f else 1f,
                        UiSwapDurationMs,
                    )
                    val onBarItem: (TeakFloatingTabItem) -> Unit = { item ->
                        val index = destinations
                            .indexOfFirst { it.key == item.key }
                            .coerceAtLeast(0)
                        if (index == selected) {
                            // 再次点击当前页 → 刷新
                            when (index) {
                                0 -> homeFeed.requestRefresh()
                                1 -> dynamicRefreshTick++
                            }
                        } else {
                            appScope.launch { tabPagerState.animateScrollToPage(index) }
                        }
                    }
                    val barSelectedColor = if (settings.colorStyle == 1) {
                        if (dark) Color.White else Color(0xFF1B1C1F)
                    } else {
                        BrandPink
                    }

                    val discVisible = !wideNavigation && settings.showDiscAfterVideo && BackgroundPlayback.active
                    val discSlot = rememberSpringFloat(
                        target = if (discVisible) 1f else 0f,
                        stiffness = 300f,
                        damping = 26f,
                    )
                    // 底部唱片出现时，底栏本身的高度和宽度完全不动，只是整体向左让位。
                    //
                    // 底栏不是满屏宽：三个 Tab 每个最多 80dp，所以它只有 240dp。
                    // 有唱片时，底栏 + 8dp 缝隙 + 唱片作为“一组”整体居中，
                    // 底栏离屏幕左边的距离 = 唱片离屏幕右边的距离。
                    val windowWidthDp = LocalConfiguration.current.screenWidthDp.dp
                    val tabBarWidth = minOf(
                        TeakFloatingTabBarDefaults.MaximumWidth,
                        TeakFloatingTabBarDefaults.MaximumItemWidth * destinations.size,
                        (windowWidthDp - 32.dp).coerceAtLeast(0.dp),
                    )
                    val discWidth = TeakFloatingTabBarDefaults.Height
                    val discPlacement = rememberDiscPlacement(destinations.size)
                    val discGap = 8.dp
                    // 整组居中后的左留白。
                    val groupLeft = ((windowWidthDp - tabBarWidth - discGap - discWidth) / 2)
                        .coerceAtLeast(0.dp)
                    val discShift = if (TeakUiStyle.isClassic) {
                        // 经典样式底栏是整条贴边，只能整体左移，让出唱片 + 右边距 + 缝隙。
                        discWidth + 16.dp + discGap
                    } else {
                        // 底栏从“自己居中”变成“跟着唱片一起居中”，只往左让半个唱片位。
                        (windowWidthDp - tabBarWidth) / 2 - groupLeft
                    }
                    // 所有页面的唱片位置统一由 rememberDiscPlacement 计算，保证和首页一致。
                    val discEndPadding = if (wideNavigation) 16.dp else discPlacement.endPadding
                    val discBottomOffset = discPlacement.bottomOffset

                    if (tabBarAlpha > 0.01f) {
                        if (wideNavigation) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                    .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 12.dp)
                                    .graphicsLayer { alpha = tabBarAlpha }.blockTouches(tabBarCovered),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                TeakFloatingTabBar(
                                    items = tabItems,
                                    selectedKey = destinations[selected].key,
                                    onItemSelected = onBarItem,
                                    layout = TeakFloatingTabLayout.Stacked,
                                    selectedContentColor = barSelectedColor,
                                )
                                androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                                com.aritxonly.teak.common.material.TeakGlassIconButton(
                                    onClick = {
                                        appScope.launch { tabPagerState.scrollToPage(0) }
                                        searchChrome.reset()
                                        showSearch = true
                                        searchQuery = ""
                                        searchSubmitted = null
                                    },
                                    size = TeakFloatingTabBarDefaults.Height,
                                ) {
                                    Icon(top.yukonga.miuix.kmp.icon.MiuixIcons.Search,
                                        contentDescription = "搜索", tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(28.dp))
                                }
                            }
                        } else if (TeakUiStyle.isClassic) {
                            // 经典样式：底栏贴着屏幕底边，上方一条分割线，不做悬浮胶囊。
                            ClassicTabBar(
                                items = tabItems,
                                selectedKey = destinations[selected].key,
                                onItemSelected = onBarItem,
                                selectedContentColor = barSelectedColor,
                                showDivider = true,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = tabBarAlpha
                                        translationX = -discSlot * discShift.toPx()
                                    }
                                    .blockTouches(tabBarCovered)
                                    .navigationBarsPadding(),
                            )
                        } else {
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        alpha = tabBarAlpha
                                        translationX = -discSlot * discShift.toPx()
                                    }
                                    .blockTouches(tabBarCovered)
                                    .navigationBarsPadding()
                                    .padding(
                                        start = 16.dp,
                                        top = 12.dp,
                                        end = 16.dp,
                                        bottom = if (settings.bottomBarRaise) {
                                            4.dp + TeakChromeDefaults.RaisedBottomBarExtra
                                        } else {
                                            4.dp
                                        },
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                TeakFloatingTabBar(
                                    items = tabItems,
                                    selectedKey = destinations[selected].key,
                                    onItemSelected = onBarItem,
                                    layout = TeakFloatingTabLayout.Stacked,
                                    selectedContentColor = barSelectedColor,
                                )
                            }
                        }

                        if (discSlot > 0.01f) {
                            BackgroundPlayDisc(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .navigationBarsPadding()
                                    .padding(end = discEndPadding)
                                    .graphicsLayer {
                                        alpha = discSlot
                                        translationX = (1f - discSlot) * 70.dp.toPx()
                                        translationY = discBottomOffset.toPx()
                                    },
                                size = TeakFloatingTabBarDefaults.Height,
                            ) { TeakNavigator.openVideo(context, it) }
                        }
                    }

                    // 换页时返回键回到"纯箭头"状态（pageKey / pageEnter 在上面声明）

                }
        }
    }
}

/**
 * 经典界面风格的底栏：没有悬浮胶囊、没有玻璃，整条贴着屏幕底边，
 * 顶部一条分割线把它和内容分开。默认的官方风格完全不会走到这里。
 */
@Composable
private fun ClassicTabBar(
    items: List<TeakFloatingTabItem>,
    selectedKey: String,
    onItemSelected: (TeakFloatingTabItem) -> Unit,
    selectedContentColor: Color,
    modifier: Modifier = Modifier,
    showDivider: Boolean = true,
) {
    val colors = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
    Row(
        modifier = modifier
            .height(58.dp)
            .background(colors.surface)
            .drawBehind {
                if (showDivider) {
                    drawLine(
                        color = colors.onBackground.copy(alpha = 0.12f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = item.key == selectedKey
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .clickable(interactionSource = null, indication = null) {
                        onItemSelected(item)
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                Icon(
                    painter = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = if (selected) selectedContentColor else colors.onBackgroundVariant,
                    modifier = Modifier.size(24.dp),
                )
                androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                Text(
                    text = item.label,
                    color = if (selected) selectedContentColor else colors.onBackgroundVariant,
                    fontSize = 10.sp,
                    fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}

