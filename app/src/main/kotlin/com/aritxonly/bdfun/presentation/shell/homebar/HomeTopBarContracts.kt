package com.aritxonly.bdfun.ui


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.user.UserProfile


@androidx.compose.runtime.Immutable
internal data class HomeTopBarContent(val profile: UserProfile?, val unreadCount: Int = 0)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarSearch(val active: Boolean = false, val query: String = "",
    val reveal: Float = 0f, val resultsVisible: Boolean = false,
    val placeholder: String = "搜索视频、UP 主")

/**
 * HyperOS 4 风格下顶栏中间的分段控件。
 * 和动态页顶部的 TeakSegmentedBar 用同一套尺寸与材质参数，保证两页观感一致。
 */
@androidx.compose.runtime.Immutable
internal data class HomeTopBarTabs(
    val options: List<String> = com.aritxonly.bdfun.HomeSectionTitles,
    val selected: Int = 0,
    val onSelect: (Int) -> Unit = {},
    /** 页面 pager 的连续位置；手指拖动时指示器跟着走，而不是等整页切完。 */
    val position: androidx.compose.runtime.State<Float>? = null,
    /** 哔哩哔哩（官方）风格：搜索框保持，分段条另起一行放在它下面。 */
    val belowSearch: Boolean = false,
)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarActions(val onSearch: () -> Unit, val onOpenMessages: () -> Unit,
    val onOpenProfile: () -> Unit, val onQueryChange: (String) -> Unit = {}, val onSubmitSearch: () -> Unit = {},
    val onCancelSearch: () -> Unit = {}, val onClearQuery: () -> Unit = {}, val onOpenSearchPage: () -> Unit = {})
internal class HomeTopBarMaterial(val wide: Boolean, val searchWidth: Dp,
    val recipe: com.aritxonly.teak.common.material.GlassMaterialRecipe,
    val backdrop: top.yukonga.miuix.kmp.blur.Backdrop?, val searchInteraction: MutableInteractionSource, val classic: Boolean)
