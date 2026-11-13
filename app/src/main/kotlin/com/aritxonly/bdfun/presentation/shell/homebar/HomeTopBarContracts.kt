package com.aritxonly.bdfun.ui


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.unit.Dp
import com.aritxonly.bdfun.core.model.user.UserProfile


@androidx.compose.runtime.Immutable
internal data class HomeTopBarContent(val profile: UserProfile?, val unreadCount: Int = 0)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarSearch(val active: Boolean = false, val query: String = "",
    val reveal: Float = 0f, val resultsVisible: Boolean = false)

/**
 * HyperOS 4 风格下顶栏中间的分段控件。
 * 和动态页顶部的 TeakSegmentedBar 用同一套尺寸与材质参数，保证两页观感一致。
 */
@androidx.compose.runtime.Immutable
internal data class HomeTopBarTabs(
    val options: List<String> = listOf("推荐", "直播", "番剧"),
    val selected: Int = 0,
    val onSelect: (Int) -> Unit = {},
)
@androidx.compose.runtime.Immutable
internal data class HomeTopBarActions(val onSearch: () -> Unit, val onOpenMessages: () -> Unit,
    val onOpenProfile: () -> Unit, val onQueryChange: (String) -> Unit = {}, val onSubmitSearch: () -> Unit = {},
    val onCancelSearch: () -> Unit = {}, val onClearQuery: () -> Unit = {}, val onOpenSearchPage: () -> Unit = {})
internal class HomeTopBarMaterial(val wide: Boolean, val searchWidth: Dp,
    val recipe: com.aritxonly.teak.common.material.GlassMaterialRecipe,
    val backdrop: top.yukonga.miuix.kmp.blur.Backdrop?, val searchInteraction: MutableInteractionSource, val classic: Boolean)
